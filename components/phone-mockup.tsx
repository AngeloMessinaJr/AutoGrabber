import type { LucideIcon } from "lucide-react"
import {
  Bot,
  ChevronDown,
  ExternalLink,
  History,
  LayoutGrid,
  Bell,
  Settings,
  ShoppingCart,
  UtensilsCrossed,
  Truck,
  Signal,
  Wifi,
  BatteryFull,
  UserCircle2,
} from "lucide-react"

type AppRow = {
  name: string
  status: string
  icon: LucideIcon
  iconColor: string
  iconBg: string
}

const apps: AppRow[] = [
  {
    name: "Instacart",
    status: "Disabled",
    icon: ShoppingCart,
    iconColor: "text-green-400",
    iconBg: "bg-green-500/15",
  },
  {
    name: "DoorDash",
    status: "Disabled",
    icon: UtensilsCrossed,
    iconColor: "text-red-400",
    iconBg: "bg-red-500/15",
  },
  {
    name: "Flex",
    status: "Disabled",
    icon: Truck,
    iconColor: "text-orange-400",
    iconBg: "bg-orange-500/15",
  },
]

function AppCard({ app }: { app: AppRow }) {
  const Icon = app.icon
  return (
    <div className="rounded-xl border border-white/5 bg-white/[0.03] p-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className={`flex size-9 items-center justify-center rounded-full ${app.iconBg}`}>
            <Icon className={`size-4.5 ${app.iconColor}`} aria-hidden="true" />
          </span>
          <span>
            <span className="block text-sm font-semibold leading-tight text-white">{app.name}</span>
            <span className="block text-[11px] font-medium text-red-400">{app.status}</span>
          </span>
        </div>
        <span className="flex h-5 w-9 items-center rounded-full bg-white/15 p-0.5">
          <span className="size-4 rounded-full bg-white/70" />
        </span>
      </div>
      <div className="mt-3 grid grid-cols-2 gap-2">
        <button
          type="button"
          className="flex items-center justify-center gap-1.5 rounded-md bg-white/[0.06] py-1.5 text-[11px] font-medium text-white/90"
        >
          <ExternalLink className="size-3" aria-hidden="true" /> Launch
        </button>
        <button
          type="button"
          className="flex items-center justify-center gap-1.5 rounded-md bg-white/[0.06] py-1.5 text-[11px] font-medium text-white/90"
        >
          <Settings className="size-3 text-primary" aria-hidden="true" /> Settings
        </button>
      </div>
    </div>
  )
}

export function PhoneMockup() {
  return (
    <div className="relative mx-auto w-[300px] max-w-full">
      {/* glow */}
      <div className="absolute -inset-6 -z-10 rounded-[3rem] bg-primary/10 blur-3xl" aria-hidden="true" />
      <div className="rounded-[2.5rem] border border-white/10 bg-neutral-950 p-2.5 shadow-2xl">
        <div className="relative overflow-hidden rounded-[2rem] bg-black">
          {/* status bar */}
          <div className="flex items-center justify-between px-5 pb-2 pt-3 text-[11px] font-medium text-white/80">
            <span className="flex items-center gap-1">
              11:56 <Settings className="size-2.5" aria-hidden="true" />
            </span>
            <span className="absolute left-1/2 top-2.5 size-3 -translate-x-1/2 rounded-full bg-neutral-800" />
            <span className="flex items-center gap-1">
              <Signal className="size-3" aria-hidden="true" />
              <Wifi className="size-3" aria-hidden="true" />
              <BatteryFull className="size-3.5" aria-hidden="true" />
            </span>
          </div>

          {/* app header */}
          <div className="flex items-center justify-between px-4 py-3">
            <Bot className="mx-auto size-6 text-primary" aria-hidden="true" />
            <UserCircle2 className="size-5 text-white/70" aria-hidden="true" />
          </div>

          {/* app list */}
          <div className="space-y-2.5 px-3 pb-2">
            {apps.map((app) => (
              <AppCard key={app.name} app={app} />
            ))}
          </div>

          {/* coming soon */}
          <div className="mx-3 mt-1 flex items-center justify-between border-t border-white/5 pb-24 pt-3">
            <span className="text-[10px] font-semibold tracking-widest text-white/40">COMING SOON</span>
            <ChevronDown className="size-3.5 text-white/40" aria-hidden="true" />
          </div>

          {/* bottom nav */}
          <div className="absolute inset-x-0 bottom-0 border-t border-white/5 bg-black/80 backdrop-blur">
            <div className="grid grid-cols-4 px-2 py-2 text-[9px] text-white/50">
              <NavItem icon={LayoutGrid} label="Dashboard" active />
              <NavItem icon={History} label="History" />
              <NavItem icon={Bell} label="Notifications" />
              <NavItem icon={Settings} label="Settings" />
            </div>
            <div className="mx-auto mb-2 h-1 w-28 rounded-full bg-white/30" />
          </div>
        </div>
      </div>
    </div>
  )
}

function NavItem({ icon: Icon, label, active }: { icon: LucideIcon; label: string; active?: boolean }) {
  return (
    <span className={`flex flex-col items-center gap-1 ${active ? "text-primary" : ""}`}>
      <Icon className="size-4" aria-hidden="true" />
      {label}
    </span>
  )
}
