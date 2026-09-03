import { NextResponse } from "next/server"
import Stripe from "stripe"
import { adminDb } from "@/lib/firebase-admin"
import { stripe } from "@/lib/stripe"

export async function POST(request: Request) {
  const signature = request.headers.get("stripe-signature")
  if (!signature) return new NextResponse("Missing signature", { status: 400 })

  let event: Stripe.Event
  try {
    event = stripe.webhooks.constructEvent(
      await request.text(),
      signature,
      process.env.STRIPE_WEBHOOK_SECRET!,
    )
  } catch {
    return new NextResponse("Invalid signature", { status: 400 })
  }

  if (event.type === "checkout.session.completed" || event.type === "checkout.session.async_payment_succeeded") {
    const session = event.data.object as Stripe.Checkout.Session
    if (session.payment_status === "paid") {
      const uid = session.metadata?.firebaseUid
      if (uid) {
        await adminDb.collection("users").doc(uid).set(
          { hasLifetimeAccess: true, lifetimeAccessGrantedAt: new Date() },
          { merge: true },
        )
      }
    }
  }

  return NextResponse.json({ received: true })
}
