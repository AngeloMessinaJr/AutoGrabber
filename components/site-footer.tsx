import Link from "next/link"

export function SiteFooter() {
  return (
    <footer className="border-t border-white/10 bg-card/30">
      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-5 py-10 text-sm text-muted-foreground md:flex-row md:items-center md:justify-between lg:px-8">
        <div><p className="font-semibold text-foreground">AutoGrabber</p><p className="mt-1 text-xs">Built for drivers who move fast.</p></div>
        <nav className="flex flex-wrap gap-5" aria-label="Footer navigation"><Link href="/features" className="hover:text-primary">Features</Link><Link href="/faq" className="hover:text-primary">FAQ</Link><Link href="/contact" className="hover:text-primary">Contact</Link></nav>
        <p className="text-xs">© 2026 AutoGrabber. Not affiliated with any delivery platform.</p>
      </div>
    </footer>
  )
}
