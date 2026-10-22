
export default function Home() {
  return (
    <main className="fixed inset-0 h-screen w-screen overflow-hidden bg-[#05070D]">
      <iframe
        src="/flame-ui.html"
        title="Flame in Freefall — PyroNova"
        className="h-full w-full border-0"
        allow="clipboard-read; clipboard-write"
      />
    </main>
  );
}
