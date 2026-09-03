import { NextResponse } from "next/server"
import type Stripe from "stripe"
import { getAdminDb } from "@/lib/firebase-admin"

export const runtime = "nodejs"
import { stripe } from "@/lib/stripe"

export async function POST(request: Request) {
  const signature = request.headers.get("stripe-signature")
  if (!signature) {
    return NextResponse.json({ error: "Missing Stripe signature." }, { status: 400 })
  }

  try {
    const event = stripe.webhooks.constructEvent(
      await request.text(),
      signature,
      process.env.STRIPE_WEBHOOK_SECRET!,
    )

    if (
      event.type === "checkout.session.completed" ||
      event.type === "checkout.session.async_payment_succeeded"
    ) {
      const session = event.data.object as Stripe.Checkout.Session
      const uid = session.metadata?.firebaseUid || session.client_reference_id
      if (uid && session.payment_status === "paid") {
        await getAdminDb().collection("users").doc(uid).set(
          { hasLifetimeAccess: true },
          { merge: true },
        )
      }
    }

    return NextResponse.json({ received: true })
  } catch (error) {
    console.error("[v0] Stripe webhook verification failed", error)
    return NextResponse.json({ error: "Invalid webhook." }, { status: 400 })
  }
}
