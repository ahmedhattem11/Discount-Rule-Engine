# Discount Rules Engine

This Scala project implements a Discount Rules Engine, designed to calculate discounts for orders based on various rules. The rules include considerations such as expiry date, product type, order date, quantity, channel, and payment method.

## Table of Contents

- [Overview](#overview)
- [Setup](#setup)
- [Usage](#usage)
- [Rules](#rules)
- [Database](#database)
- [Logging](#logging)
- [Dependencies](#dependencies)
- [Contributing](#contributing)
- [License](#license)

## Overview

Discount Rules Engine is a Scala application that reads order data from a CSV file, applies discount rules to each order, calculates the discounts, and stores the processed data in an Oracle database. The project uses Apache Log4j for logging and JDBC for database connectivity.

## Setup

To set up the project, follow these steps:

1. Clone the repository to your local machine.
2. Ensure you have Scala and SBT installed.
3. Add the Oracle JDBC driver (ojdbc6.jar) to the `lib` directory of the project.
4. Configure the `log.xml` file for logging preferences.
5. Ensure an Oracle database is available to connect to.

## Usage

To use the Discount Rules Engine:

1. Place your order data in a CSV file (e.g., `TRX1000.csv`) inside the `src/main/resources` directory.
2. Run the `DiscountRulesEngine` object.
3. Check the logs for information about the application's execution.
4. Verify the database for processed order data.

## Rules

The Discount Rules Engine applies the following rules to calculate discounts:

1. **Expiry Qualification**: If the order's expiry date is less than 30 days from the order date, apply a discount based on the remaining days until expiry.
2. **Wine or Cheese Discount**: Apply a discount if the product is either wine or cheese.
3. **March 23rd Discount**: Apply a discount if the order date is March 23rd.
4. **Quantity Discount**: Apply a discount based on the quantity bought.
5. **App Discount**: Apply a discount if the channel is through the app.
6. **Visa Discount**: Apply a discount if the payment method is Visa.

## Database

The project creates a table named `discountOrders` in the Oracle database to store processed order data. The table schema includes fields for order details, price before discount, discount amount, and price after discount.

## Logging

The project uses Log4j for logging. Logs are configured in the `log.xml` file and stored in the `logs` directory.

## Dependencies

The project has the following dependencies:

- Scala 2.13.13
- Apache Log4j 2.14.1

## Contributing

Contributions to this project are welcome. You can fork the repository, make your changes, and submit a pull request.

## License

This project is licensed under the [MIT License](LICENSE).
```
