import { Download } from "lucide-react"

const steps = [
  { num: 1, label: "Download the APK", color: "bg-primary text-primary-foreground" },
  { num: 2, label: "Install from downloads folder", color: "bg-blue-500 text-white" },
  { num: 3, label: "Launch & setup preferences", color: "bg-fuchsia-500 text-white" },
]

const platforms = ["Spark", "DoorDash", "Uber", "Instacart", "Amazon Flex"]

export function InstallDownload() {
  return (
    <section className="mx-auto max-w-6xl px-4 py-16 md:px-6 md:py-20">
      {/* platforms banner */}
      <div className="rounded-2xl border border-white/8 bg-card/60 px-6 py-5 text-center">
        <p className="text-sm font-medium text-white/90">
          AutoGrabber supports these platforms:{" "}
          <span className="font-semibold text-primary">{platforms.join(", ")}</span>.
        </p>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        {/* installation process */}
        <div className="rounded-2xl border border-white/8 bg-card/60 p-6 md:p-8">
          <h3 className="text-xl font-semibold text-white">App Installation Process</h3>
          <p className="mt-1 text-sm text-muted-foreground">Download, install, and setup platform preferences.</p>
          <ol className="mt-6 space-y-3">
            {steps.map((step) => (
              <li
                key={step.num}
                className="flex items-center gap-4 rounded-xl border border-white/8 bg-white/[0.02] px-4 py-3.5"
              >
                <span className={`flex size-7 shrink-0 items-center justify-center rounded-full text-xs font-bold ${step.color}`}>
                  {step.num}
                </span>
                <span className="text-sm font-medium text-white">{step.label}</span>
              </li>
            ))}
          </ol>
        </div>

        {/* download card */}
        <div id="download" className="flex flex-col rounded-2xl border border-white/8 bg-card/60 p-6 md:p-8">
          <div className="flex items-start justify-between">
            <div>
              <h3 className="text-xl font-semibold text-white">AutoGrabber APK</h3>
              <p className="mt-1 text-sm text-muted-foreground">Minimum Requirement: Android 15+</p>
            </div>
            <span className="rounded-full border border-primary/50 px-3 py-1 text-xs font-semibold tracking-wide text-primary">
              FREE
            </span>
          </div>
          <div className="mt-auto pt-8">
            <a
              href="#"
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-3.5 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90"
            >
              <Download className="size-4" aria-hidden="true" />
              Download ~121 MB
            </a>
          </div>
        </div>
      </div>
    </section>
  )
}
