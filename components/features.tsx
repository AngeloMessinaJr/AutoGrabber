import { CheckCircle2, EyeOff } from "lucide-react"

const features = [
  {
    icon: CheckCircle2,
    title: "Accept & Reject Offers",
    description: "Automatically accept and reject offers based on your preferences.",
  },
  {
    icon: EyeOff,
    title: "Block Stores on Instacart",
    description: "Automatically hide batches from blocked stores on Instacart.",
  },
]

export function Features() {
  return (
    <section id="features" className="relative border-y border-white/5">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "linear-gradient(120deg, oklch(0.82 0.13 205 / 0.08), transparent 40%, oklch(0.55 0.2 260 / 0.1))",
        }}
        aria-hidden="true"
      />
      <div className="relative mx-auto max-w-6xl px-4 py-16 md:px-6 md:py-20">
        <p className="text-xs font-semibold tracking-[0.3em] text-primary">FEATURES</p>
        <div className="mt-8 grid gap-5 md:grid-cols-2">
          {features.map((f) => {
            const Icon = f.icon
            return (
              <article
                key={f.title}
                className="rounded-2xl border border-white/8 bg-card/60 p-6 backdrop-blur-sm transition-colors hover:border-primary/30"
              >
                <span className="flex size-11 items-center justify-center rounded-xl bg-primary/15">
                  <Icon className="size-5 text-primary" aria-hidden="true" />
                </span>
                <h3 className="mt-4 text-lg font-semibold text-white">{f.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{f.description}</p>
              </article>
            )
          })}
        </div>
      </div>
    </section>
  )
}
