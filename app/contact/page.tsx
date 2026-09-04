import { Mail, MessageCircle } from "lucide-react"
import Link from "next/link"
import { SiteFooter } from "@/components/site-footer"
import { SiteHeader } from "@/components/site-header"

export default function ContactPage() {
  return (
    <div className="flex min-h-dvh flex-col bg-background text-foreground">
      <SiteHeader />
      <main className="mx-auto flex w-full max-w-7xl flex-1 flex-col px-5 py-16 lg:px-8 lg:py-24">
        <div className="max-w-2xl">
          <p className="font-mono text-xs font-bold uppercase tracking-[0.24em] text-primary">Contact AutoGrabber</p>
          <h1 className="mt-4 text-balance text-4xl font-semibold tracking-tight text-white md:text-6xl">Let&apos;s get you moving.</h1>
          <p className="mt-6 max-w-xl text-pretty text-base leading-7 text-muted-foreground md:text-lg">Need help with setup, access, or your account? Send us a note and we&apos;ll point you in the right direction.</p>
        </div>
        <div className="mt-12 grid gap-4 md:grid-cols-2">
          <a href="mailto:support@autograbber.app" className="group rounded-2xl border border-white/10 bg-card/60 p-6 transition-colors hover:border-primary/50 hover:bg-card">
            <span className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary"><Mail className="size-5" /></span>
            <h2 className="mt-6 text-xl font-semibold text-white">Email support</h2>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">For account, billing, and technical questions.</p>
            <p className="mt-5 text-sm font-semibold text-primary">support@autograbber.app</p>
          </a>
          <div className="rounded-2xl border border-white/10 bg-card/60 p-6">
            <span className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary"><MessageCircle className="size-5" /></span>
            <h2 className="mt-6 text-xl font-semibold text-white">Before you write</h2>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">Include the email on your AutoGrabber account and a short description of what you need help with.</p>
            <Link href="/faq" className="mt-5 inline-flex text-sm font-semibold text-primary hover:underline">Read the FAQ first →</Link>
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
