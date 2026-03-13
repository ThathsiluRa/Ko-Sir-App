# Ko-Sir AI — Sri Lanka Home Tutor Booking Platform

A web platform for connecting students with home tutors in Sri Lanka. Students can search for tutors, book sessions, pay online, and chat with their tutor — all in one place.

Built with **Java Spring Boot** + **Thymeleaf** + **H2 in-memory database**.

---

## What it does

- Students register, search for tutors by subject/location, and book sessions
- Tutors set their profile (subjects, rate, teaching mode: home visit / fixed location / both)
- Payment via **PayHere** (Sri Lankan payment gateway) before booking is confirmed
- After payment, tutor sees student's contact info, address, and parent contact
- In-app chat between student and tutor per booking
- Admin panel to manage users, bookings, and configure all platform settings (SMS, email, PayHere)
- OTP verification on registration (SMS via text.lk or Twilio, + email)

---

## Prerequisites

Make sure you have these installed before running:

| Tool | Version | Check with |
|------|---------|------------|
| Java JDK | 17 or higher | `java -version` |
| Git | Any recent | `git --version` |

That's it — **no database setup needed**, no separate Maven install needed. The project includes the Maven wrapper (`mvnw`) and uses an embedded H2 database.

---

## Running locally

**1. Clone the repo**
```bash
git clone https://github.com/ThathsiluRa/Ko-Sir-App.git
cd Ko-Sir-App/tutor-booking-platform
```

**2. Start the app**

On Windows:
```bash
mvnw.cmd spring-boot:run
```

On Mac/Linux:
```bash
./mvnw spring-boot:run
```

The first run will download Maven and all dependencies automatically (~2 minutes). After that it starts in a few seconds.

**3. Open the app**

Go to [http://localhost:8080](http://localhost:8080)

---

## Demo accounts

The app seeds these accounts on every startup so you can test immediately:

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@kosir.lk | admin123 |
| Tutor | amali@kosir.lk | tutor123 |
| Tutor | kasun@kosir.lk | tutor123 |
| Tutor | dilini@kosir.lk | tutor123 |
| Student | nimesh@kosir.lk | student123 |

> **Note:** The database resets every time you restart the app (H2 in-memory). Any accounts you create or bookings you make will be gone after a restart. This is intentional for development — the demo data is always fresh.

---

## Admin panel

Log in as admin and go to [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard)

From there you can:
- Approve/reject tutor profiles
- View and manage all bookings
- Configure SMS, email, and PayHere settings under **Settings**

### Setting up PayHere (optional)

To enable real payments, go to **Admin → Settings** and fill in:
- **Merchant ID** and **Merchant Secret** from your [PayHere account](https://www.payhere.lk)
- Set mode to **Sandbox** for testing, **Live** for production
- Toggle **Enable PayHere Payments** on

Without PayHere configured, the app uses a demo payment page instead.

### Setting up SMS (optional)

Go to **Admin → Settings → SMS Configuration** and pick your provider:
- **text.lk** — enter your API token and Sender ID
- **Twilio** — enter Account SID, Auth Token, and From number

### Setting up email (optional)

Go to **Admin → Settings → Email Configuration** and fill in your SMTP details (works with Gmail, Outlook, etc.)

---

## Project structure

```
src/main/java/com/tutorplatform/
├── config/          # Spring Security, password encoder, data initializer
├── controller/      # HTTP request handlers (one per feature area)
├── model/           # Database entities (User, Booking, TutorProfile, etc.)
├── repository/      # Database queries (Spring Data JPA)
├── service/         # Business logic (booking, payments, SMS, email)
└── dto/             # Data transfer objects

src/main/resources/
├── templates/       # Thymeleaf HTML templates
│   ├── admin/       # Admin dashboard, manage users/bookings/settings
│   ├── booking/     # Book a session, payment, chat
│   ├── student/     # Student dashboard, search, my bookings
│   ├── tutor/       # Tutor dashboard, profile, my sessions
│   └── layout/      # Shared navbar, base layout
└── application.properties  # App configuration
```

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2.3 |
| Security | Spring Security (form login, BCrypt passwords, CSRF protection) |
| Database | H2 (in-memory, resets on restart) |
| ORM | Spring Data JPA / Hibernate |
| Templates | Thymeleaf |
| Frontend | Bootstrap 5, Bootstrap Icons |
| Payments | PayHere (Sri Lankan payment gateway) |
| SMS | text.lk / Twilio |
| Email | Spring Mail (SMTP) |

---

## Viewing the database

While the app is running, visit [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

Use these connection settings:
- **JDBC URL:** `jdbc:h2:mem:tutordb`
- **Username:** `sa`
- **Password:** *(leave empty)*

---

## Known limitations

- H2 in-memory database — data doesn't persist between restarts. For production, swap to PostgreSQL or MySQL by changing `application.properties` and the JPA dialect.
- PayHere notify URL must be publicly accessible for real payment confirmation. In local dev, payments won't auto-confirm unless you use a tunneling tool like [ngrok](https://ngrok.com).
- OTP verification requires SMS/email credentials configured in Admin Settings to actually send codes. Without them, OTPs will only appear in the server console logs.
