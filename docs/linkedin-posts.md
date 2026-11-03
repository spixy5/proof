# LinkedIn post drafts

Three posts, one per stage. Each shows a different skill. Swap in your repo link, and attach
the image noted at the top of each one. Post them a few days apart.

---

## Post 1: the launch and the architecture

📎 *Attach: `docs/images/architecture.svg` (export as PNG), plus 2 or 3 screenshots as a carousel*

---

🍞 I built a baking app, and it taught me more about architecture than any tutorial.

It's called **Proof** (as in proofing dough). It's a recipe manager for people who bake. The feature I'm proudest of is the smart scaler.

You find a sourdough recipe for 2 loaves but you want 3. Proof rescales every ingredient in metric, and it does more than multiply:

→ 1250 g becomes 1.25 kg, and 6 tbsp becomes 90 ml
→ yeast and salt keep half-gram precision, because 2 g vs 2.5 g matters
→ eggs never drop below half an egg
→ baker's percentages stay the same at any batch size

All of that logic lives in a pure Kotlin module with no Android imports, covered by 25+ unit tests. Making the most important code the easiest to test was the best decision I made on this project.

The stack:
📱 Kotlin + Jetpack Compose, MVVM, Room, Retrofit, WorkManager
☁️ Spring Boot 4 + PostgreSQL, JWT auth, documented with Swagger
⚙️ GitHub Actions builds and tests both sides on every push

Code, architecture diagram and APK: [link]

What's the one feature you'd add to a baking app? 👇

#AndroidDev #Kotlin #JetpackCompose #SpringBoot #SoftwareArchitecture

---

## Post 2: offline-first sync

📎 *Attach: a screen recording. Turn on airplane mode, favourite 3 recipes, turn the network back on, then open the app on a second emulator and the favourites appear.*

---

✈️ My app works in airplane mode. Here's how, and why it was harder than I expected.

Kitchens and supermarket basements are bad places for Wi-Fi, so I built **Proof** to be offline-first. The rule I set: **the UI never talks to the network.**

How it works:

1️⃣ Room is the single source of truth. Every screen observes a database Flow.
2️⃣ When you tap ♥, the app writes locally and marks the row `pendingSync`. The UI updates instantly.
3️⃣ A WorkManager job waits for a connection, pushes pending changes, then pulls the server's copy.
4️⃣ Rows with pending changes keep the local value. Everything else takes the server's value, so a favourite you added on your tablet shows up on your phone.

The part that caught me out was **retries**. When a connection drops mid-request, the client can't tell whether the server got it. So I made the API's PUT and DELETE idempotent: sending the same request twice gives the same result. An integration test sends every request twice to check this.

Another small detail I like: you can favourite recipes as a guest, and when you sign in later, everything uploads.

Offline-first isn't a library you add. It's a decision about where the truth lives.

Repo: [link]

#AndroidDev #OfflineFirst #Kotlin #MobileArchitecture #Room

---

## Post 3: the release pipeline

📎 *Attach: a screenshot of the GitHub Release page with the APK, or of the green Actions run*

---

🚀 Proof v1.0.0 is out, and no human built the APK.

The last piece of my baking app project was the release pipeline:

`git tag v1.0.0 && git push --tags`

→ GitHub Actions runs the unit tests
→ builds a minified release APK with R8
→ signs it with a key kept in repository secrets
→ publishes it to GitHub Releases with auto-generated release notes

Every push also runs:
✅ Android: unit tests, lint, debug APK
✅ Backend: MockMvc integration tests for auth, recipes and favourites, plus a Docker image build

`docker compose up` starts PostgreSQL and the Spring Boot API with seeded recipes, so anyone can run the full stack in one command.

Things I learned:
• Automate the release on day one, not at the end
• A README with a diagram and screenshots does more for a portfolio than another feature
• "It works on my machine" stops being an excuse once CI builds everything

You can download the APK and try it here: [link]

Thanks to everyone who followed along with this build. Next up: scaling a recipe by the flour you have left 🥖

#AndroidDev #GitHubActions #CICD #SpringBoot #BuildInPublic
