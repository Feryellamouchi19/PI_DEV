# Event Management Module – PI_DEV

## Overview

This module was developed as part of the **PIDEV – 3rd Year Engineering Program at Esprit School of Engineering (Academic Year 2025–2026)**.

The **Event Management Module** allows administrators to create, manage, and organize events within the platform.
It provides an intuitive interface for managing event details, schedules, and event-related information.

The module is implemented as a **desktop application using JavaFX**, following a layered architecture that separates the user interface, business logic, and data access.

## Features

* Create new events
* Modify existing events
* Delete events
* Display the list of events
* View event details
* Add programs/schedules to events
* Generate event posters using AI
* User-friendly graphical interface

## Tech Stack
* JavaFX
* FXML
* Scene Builder
* CSS
* Java
* MySQL
* Maven

## Architecture

The module follows a **3-layer architecture**:

* **Presentation Layer**

  * JavaFX interfaces (FXML)
  * Controllers handling user interactions

* **Business Logic Layer**

  * Services managing application logic

* **Data Layer**

  * Entities representing the data model
  * Database access through services

Main components include:

* `Evenement` entity
* `Programme` entity
* Controllers for event management
* Services handling CRUD operations

## Contributors

* **Feryel Lamouchi** – Event Management Module

## Academic Context

Developed at **Esprit School of Engineering – Tunisia**
PIDEV – 3rd Year Engineering Program | 2025–2026

## Getting Started

1. Clone the repository:

```bash
git clone https://github.com/Feryellamouchi19/PI_DEV
```

2. Open the project using **IntelliJ IDEA**.

3. Install Maven dependencies:

```bash
mvn clean install
```

4. Run the application:

```bash
mvn javafx:run
```

## Acknowledgments

This project was developed as part of the **PIDEV academic project** at Esprit School of Engineering.
