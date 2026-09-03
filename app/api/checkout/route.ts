import { NextResponse } from "next/server"
import { stripe } from "@/lib/stripe"
import { LIFETIME_PRODUCT } from "@/lib/products"
import { getAdminAuth } from "@/lib/firebase-admin"

export const runtime = "nodejs"

export async function POST(request: Request) {
  try {
    const authHeader = request.headers.get("authorization")
    const idToken = authHeader?.startsWith("Bearer ") ? authHeader.slice(7) : null
    if (!idToken) return NextResponse.json({ error: "Authentication required." }, { status: 401 })
    const token = await getAdminAuth().verifyIdToken(idToken)

    const body = await request.json()
    if (body.uid && body.uid !== token.uid) return NextResponse.json({ error: "Invalid request." }, { status: 403 })
    const uid = token.uid

    const forwardedHost = request.headers.get("x-forwarded-host")
    const forwardedProto = request.headers.get("x-forwarded-proto") || "https"
    const origin = forwardedHost
      ? `${forwardedProto}://${forwardedHost.split(",")[0].trim()}`
      : request.headers.get("origin") || new URL(request.url).origin
    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      line_items: [
        {
          price_data: {
            currency: "usd",
            product_data: {
              name: LIFETIME_PRODUCT.name,
              description: LIFETIME_PRODUCT.description,
            },
            unit_amount: LIFETIME_PRODUCT.priceInCents,
          },
          quantity: 1,
        },
      ],
      success_url: `${origin}/account?purchase=success`,
      cancel_url: `${origin}/account?purchase=canceled`,
      client_reference_id: uid,
      metadata: { firebaseUid: uid },
    })
    return NextResponse.json({ url: session.url })
  } catch (error) {
    console.error("[v0] checkout session creation failed", error)
    const message = error instanceof Error ? error.message : "Unable to start checkout."
    return NextResponse.json(
      { error: `${message} Please try again.` },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    )
  }
}
