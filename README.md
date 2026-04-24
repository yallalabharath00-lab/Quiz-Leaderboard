# Quiz Leaderboard System

## About this project

This project is part of an internship assignment where I had to work with an external API and build a leaderboard system.

The API simulates a quiz competition where participants get scores across multiple rounds. The challenge was not just collecting the data, but handling **duplicate responses**, which can happen in real-world systems.

---

## What I did

* Called the API 10 times (poll 0 to 9)
* Collected all the quiz events
* Removed duplicate entries using `(roundId + participant)`
* Calculated total scores for each participant
* Sorted them to create a leaderboard
* Submitted the final result back to the API

---

## Key challenge

The main tricky part was **handling duplicate data**.

Sometimes the same event appears again in different API responses.
If we don’t filter it, scores become incorrect.

So I used a `Set` to track unique entries and made sure each `(roundId + participant)` is counted only once.

---

## How it works (simple flow)

1. Fetch data from API (10 times)
2. Store all events
3. Remove duplicates
4. Calculate scores
5. Sort leaderboard
6. Submit result

---

## Tech used

* Java (core)
* Java HTTP Client
* org.json library

---

## How to run

1. Clone the repo

2. Add your register number in the code:

   ```java
   private static final String REG_NO = "YOUR_REG_NO";
   ```

3. Compile:

   ```
   javac -cp ".;lib/*" QuizLeaderboard.java
   ```

4. Run:

   ```
   java -cp ".;lib/*" QuizLeaderboard
   ```

---

## Output

* Prints leaderboard in sorted order
* Shows total score
* Displays API response after submission

---

## What I learned

* How APIs work in real-world scenarios
* Why duplicate data handling is important
* Basic backend data processing
* Writing clean aggregation logic

---

## Note

* There is a **5-second delay** between each API call (as required)
* All 10 polls must be completed to get correct data

---
