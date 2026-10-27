import './style.css';
export const metadata = { title: 'Only Me No Puppy', description: '나만의 자율적인 반려동물' };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="ko"><body>{children}</body></html>;
}
