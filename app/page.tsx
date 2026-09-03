import { SiteHeader } from "@/components/site-header"
import { Hero } from "@/components/hero"
import { Features } from "@/components/features"
import { InstallDownload } from "@/components/install-download"
import { SiteFooter } from "@/components/site-footer"

export default function Page() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <SiteHeader />
      <main className="flex-1">
        <Hero />
        <Features />
        <InstallDownload />
      </main>
      <SiteFooter />
    </div>
  )
}
