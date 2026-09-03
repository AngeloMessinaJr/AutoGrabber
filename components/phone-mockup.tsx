import Image from "next/image"

const appScreenshot =
  "https://hebbkx1anhila5yf.public.blob.vercel-storage.com/Screenshot_20260903_124135_AutoGrabber-7dOQ6aUUqa7B5dPAeyK5RZJZtHrZrk.jpg"

export function PhoneMockup() {
  return (
    <div className="relative mx-auto w-[300px] max-w-full sm:w-[340px]">
      <div className="absolute -inset-8 -z-10 rounded-[3rem] bg-primary/10 blur-3xl" aria-hidden="true" />
      <div className="overflow-hidden rounded-[2.5rem] border border-white/10 bg-neutral-950 p-2 shadow-2xl">
        <Image
          src={appScreenshot}
          alt="AutoGrabber app dashboard showing Instacart, DoorDash, and Flex controls"
          width={1080}
          height={2160}
          unoptimized
          priority
          className="h-auto w-full rounded-[2rem]"
        />
      </div>
    </div>
  )
}

export default PhoneMockup
