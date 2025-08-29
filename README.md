Please use the following dummy account to see a loaded dashboard.  
email: test@gmail.com  
password: Cupcakes123  

You may need to use the back button if the nav bar doesn't open something.  

# Budget Hero

Budget Hero is a minimalist, user-friendly Android budgeting app designed to help users efficiently track expenses, income, fixed expenses, and financial goals. The app uses a pastel-inspired color palette and a clear, accessible interface to make personal finance less intimidating and more actionable for students and young professionals.

---

## App Purpose

Budget Hero empowers users to take control of their finances with an intuitive dashboard, visualizations (bar charts, pie charts), and fast entry of daily transactions. Users can categorize income and expenses, set savings goals, monitor progress, and visually analyze their spending patterns over any selected time period. Design decisions focused on reducing clutter, making category selection seamless, and providing instant feedback on budget progress.

---

## Own Features

**1. Top 3 Frequent Expenses Visualization:**  
Budget Hero automatically identifies and displays your three most frequently recorded expense categories in a dedicated bar chart on the dashboard. This helps users quickly spot spending patterns and address recurring costs. The bar graph is clearly labeled and updates in real time as new transactions are added.

**2. Fixed Expenses Management & Reporting:**  
Users can add, edit, or remove fixed (recurring) monthly expenses (such as rent, subscriptions, or insurance) which are stored separately from variable expenses. These fixed expenses are integrated into all balance calculations, statements, and reports. Adjusting fixed expenses instantly updates your projected monthly balance and analytics, giving a clear picture of committed versus discretionary spending.

**3. Dark Mode Support:**  
Budget Hero includes a fully implemented dark mode theme. Users benefit from a modern, comfortable interface that adapts to system dark mode settings, reducing eye strain and improving usability at night or in low-light conditions.

---

## App Design Decisions

- Clear separation of **fixed** and **variable** expenses for more accurate financial planning.
- Automatic display of the **Top 3 Most Frequent Expenses** so users can instantly recognize their main spending habits.
- **Dark mode** provides a visually comfortable experience in all environments.
- Graphical analytics and reporting ensure that financial summaries are understandable at a glance.
- Use of custom colors and intuitive layouts makes navigation and category selection simple and enjoyable.

---


## GitHub Actions: Automated Build/Tests

This repository includes a `.github/workflows/android.yml` workflow file.  
The workflow is triggered on every push or pull request to `main` and runs a full Gradle build and unit test suite in a clean GitHub Actions Ubuntu VM.

Workflow file:
```
# File: .github/workflows/android.yml

name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:

    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: 17

      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew

      - name: Build with Gradle
        run: ./gradlew build

      # Optional: Run tests
      - name: Run unit tests
        run: ./gradlew test
```


## App Icon & Image Assets

### App Icon:
The Budget Hero app icon features a simple, modern hero shield with a currency symbol at the center, using the app’s pastel palette for instant recognition and a friendly vibe.

(Replace with your actual icon screenshot or image.)
### Image Assets:
The only image asset is the app icon. And the custom font is also an additional asset.


## Demo Video

https://youtu.be/GIwkgxVB_Js

The demo was done in bluestacks 5 because I did not have access to a physical android device. It was incredibly hard trying to convince someone to install an apk on their phone so I used bluestacks 5.  I tried telling my family member how to install it over the phone, but then they would have had to do the demo, and I wanted to do it by myself. 





## Attributions

- Firebase real-time data read/write patterns adapted from:
  - Vikram Kodag, "Firebase real-time data read-write best practices - Android"
    https://medium.com/@kodagvikram/firebase-real-time-data-read-write-best-practices-android-67a06fa6420d
    Accessed: 2025-06-09

- Firebase Documentation, "Read and Write Data on Android"
    https://firebase.google.com/docs/database/android/read-and-write
    Accessed: 2025-06-09

- Subcollection handling in Firestore adapted from:
  - StackOverflow, "How to add subcollection to a document in Firebase Cloud Firestore"
    https://stackoverflow.com/questions/47514419/how-to-add-subcollection-to-a-document-in-firebase-cloud-firestore
    Accessed: 2025-06-09

- Glide image loading implementation adapted from:
  - Eran Gross, "Introduction to Glide: The Image Loading Library for Android"
    https://medium.com/@eran_6323/introduction-to-glide-the-image-loading-library-for-android-a3b9b0fc39a7
    Accessed: 2025-06-09

- Bar chart and pie chart implementation using MPAndroidChart adapted from:
  - Malcolm Maima, "(Kotlin) Implementing a Barchart and Piechart using MPAndroidChart"
    https://malcolmmaima.medium.com/kotlin-implementing-a-barchart-and-piechart-using-mpandroidchart-8c7643c4ba75
    Accessed: 2025-06-09

- Spinner implementation and simplification in Android adapted from:
  - Nelson Leme, "Simplifying Using Spinners in Android"
    https://medium.com/geekculture/simplifying-using-spinners-in-android-ad14f8f1213d
    Accessed: 2025-06-09

- RecyclerView item onClick implementation adapted from:
  - StackOverflow, "how to make the items of my Firebase Recycler adapter onClick with kotlin android"
    https://stackoverflow.com/questions/70528244/how-to-make-the-items-of-my-firebase-recycler-adapter-onclick-with-kotlin-android
    Accessed: 2025-06-09

- Hiding a button programmatically in Android adapted from:
  - StackOverflow, "How to hide a button programmatically?"
    https://stackoverflow.com/questions/70528244/how-to-make-the-items-of-my-firebase-recycler-adapter-onclick-with-kotlin-android
    Accessed: 2025-06-09

- GitHub Actions CI/CD workflow for Kotlin adapted from:
  - Nishit Bakraniya, "Kotlin CI/CD Script Using GitHub Actions"
    https://medium.com/@starlord125/kotlin-ci-cd-script-using-github-actions-302f9ea49874
    Accessed: 2025-06-09

