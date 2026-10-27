'use client';
import { useCallback, useEffect, useState } from 'react';

const API = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';
type State = { petId: string; hunger: number; energy: number; mood: number; bond: number; stress: number; action: string; simulatedUntil: string };
type Pet = { id: string; name: string; species: 'DOG' | 'CAT' };
type Snapshot = { pet: Pet | 'none'; state?: State; asOf?: string };
type Event = { occurred_at: string; event_type: string; detail: string };

export default function Home() {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [events, setEvents] = useState<Event[]>([]);
  const [name, setName] = useState('');
  const [species, setSpecies] = useState<'DOG' | 'CAT'>('DOG');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const load = useCallback(async () => {
    try {
      const response = await fetch(`${API}/pets/mine`, { cache: 'no-store' });
      if (!response.ok) throw new Error('서버에 연결할 수 없습니다');
      const data: Snapshot = await response.json();
      setSnapshot(data);
      if (data.pet !== 'none') {
        const history = await fetch(`${API}/pets/${data.pet.id}/events`);
        if (history.ok) setEvents(await history.json());
      }
    } catch (error) { setMessage((error as Error).message); }
  }, []);
  useEffect(() => { load(); const timer = setInterval(load, 30000); return () => clearInterval(timer); }, [load]);

  async function adopt() {
    setBusy(true); setMessage('');
    try {
      const response = await fetch(`${API}/pets`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name, species }) });
      if (!response.ok) throw new Error((await response.json()).message);
      await load();
    } catch (error) { setMessage((error as Error).message); } finally { setBusy(false); }
  }
  async function act(kind: string) {
    if (!snapshot || snapshot.pet === 'none') return;
    setBusy(true); setMessage('');
    try {
      const response = await fetch(`${API}/pets/${snapshot.pet.id}/actions/${kind}`, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() } });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message);
      setMessage(data.event.detail);
      await load();
    } catch (error) { setMessage((error as Error).message); } finally { setBusy(false); }
  }

  return <main className="shell">
    <header><span className="brand">ONLY ME NO PUPPY</span><span className="status">● HOME CAM</span></header>
    {snapshot === null ? <p>반려동물을 불러오는 중…</p> : snapshot.pet === 'none' ? <section className="adopt card">
      <span className="eyebrow">새로운 가족</span><h1>어떤 친구를 만날까요?</h1><p>당신이 없는 동안에도 자신만의 하루를 보내는 친구를 입양해요.</p>
      <div className="species"><button className={species === 'DOG' ? 'selected' : ''} onClick={() => setSpecies('DOG')}>🐶 강아지</button><button className={species === 'CAT' ? 'selected' : ''} onClick={() => setSpecies('CAT')}>🐱 고양이</button></div>
      <label>이름<input value={name} maxLength={30} onChange={e => setName(e.target.value)} placeholder="이름을 지어주세요" /></label>
      <button className="primary" onClick={adopt} disabled={busy || !name.trim()}>입양하기</button>
    </section> : <><section className="hero"><div><span className="eyebrow">우리 집의 오늘</span><h1>{snapshot.pet.name}의 하루</h1><p>접속하지 않아도 스스로 생활하고 있어요.</p></div><span className="time">최근 동기화 {snapshot.asOf && new Date(snapshot.asOf).toLocaleString('ko-KR')}</span></section>
      <div className="grid"><section className="camera card"><div className="camera-top"><strong>거실 홈캠</strong><span>LIVE 상태</span></div><div className="scene"><div className="window"/><div className="sun"/><div className="pet">{snapshot.pet.species === 'DOG' ? '🐕' : '🐈'}</div><div className="rug"/></div><div className="camera-caption"><strong>{snapshot.pet.name}</strong><span>{actionLabel(snapshot.state?.action)}</span></div></section>
      <section className="care card"><span className="eyebrow">돌봄</span><h2>지금 함께하기</h2><div className="stats">{snapshot.state && [['배고픔', snapshot.state.hunger], ['에너지', snapshot.state.energy], ['기분', snapshot.state.mood], ['친밀도', snapshot.state.bond]].map(([label, value]) => <div className="stat" key={label}><div><span>{label}</span><b>{value}</b></div><progress value={Number(value)} max="100" /></div>)}</div><div className="actions"><button disabled={busy} onClick={() => act('feed')}>🥣 먹이 주기</button><button disabled={busy} onClick={() => act('play')}>🧸 함께 놀기</button><button disabled={busy} onClick={() => act('train')}>⭐ 훈련하기</button></div></section></div>
      <section className="history card"><span className="eyebrow">부재 중 기록</span><h2>{snapshot.pet.name}가 보낸 시간</h2>{events.length ? <ul>{events.map((event, index) => <li key={index}><time>{new Date(event.occurred_at).toLocaleString('ko-KR')}</time><span>{event.detail}</span></li>)}</ul> : <p>아직 기록이 없습니다.</p>}</section></>}
    {message && <div className="toast" role="status">{message}</div>}
  </main>;
}
function actionLabel(action?: string) { return ({ SLEEP: '자고 있어요', EXPLORE: '집을 탐색하고 있어요', PLAY_ALONE: '혼자 놀고 있어요', REST: '쉬고 있어요', LOOKING_FOR_FOOD: '먹이를 찾고 있어요', EATING: '밥을 먹고 있어요', PLAYING_TOGETHER: '함께 놀고 있어요', TRAINING: '훈련하고 있어요' } as Record<string, string>)[action || ''] || '생활하고 있어요'; }
