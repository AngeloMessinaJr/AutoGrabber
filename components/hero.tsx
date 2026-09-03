import { PhoneMockup } from "@/components/phone-mockup"

export function Hero() {
  return (
    <section className="relative overflow-hidden">
      {/* subtle radial backdrop */}
      <div
        className="pointer-events-none absolute inset-0 -z-10"
        style={{
          background:
            "radial-gradient(ellipse 80% 60% at 70% 20%, oklch(0.82 0.13 205 / 0.08), transparent 60%)",
        }}
        aria-hidden="true"
      />
      <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 py-16 md:grid-cols-2 md:px-6 md:py-24 lg:py-28">
        <div className="text-center md:text-left">
          <h1 className="text-4xl font-bold tracking-tight text-balance text-white md:text-5xl lg:text-6xl">
            Meet AutoGrabber!
          </h1>
          <p className="mx-auto mt-6 max-w-md text-pretty text-base leading-relaxed text-muted-foreground md:mx-0 md:text-lg">
            Maximize your earnings. AutoGrabber automatically accepts and rejects offers, allowing you to drive
            safely.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3 md:justify-start">
            <a
              href="#download"
              className="rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
            >
              Download for Android
            </a>
            <a
              href="#features"
              className="rounded-lg border border-white/10 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-white/5"
            >
              See features
            </a>
          </div>
        </div>
        <div className="flex justify-center">
          <PhoneMockup />
        </div>
      </div>
    </section>
  )
}
