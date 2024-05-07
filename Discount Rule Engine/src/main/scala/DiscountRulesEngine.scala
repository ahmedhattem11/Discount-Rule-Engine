import java.sql.DriverManager
import scala.io.Source
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.apache.logging.log4j.LogManager

object DiscountRulesEngine extends App {

      // Initialize logger
      val logger = LogManager.getLogger(getClass.getName)

      // Connect to Oracle database
      logger.info("Connecting to the database")
      val url = "jdbc:oracle:thin:@//localhost:1521/XE"
      val driver = "oracle.jdbc.OracleDriver"
      val username = "hr"
      val password = "123"

      Class.forName(driver)
      val connection = DriverManager.getConnection(url, username, password)

      // Test the connection
      if (connection != null) {
            logger.info("Connected to the database")
      } else {
            logger.error("Failed to make connection to the database")
      }

      // Read the lines from the file
      logger.info("Reading data from the file")
      val lines = Source.fromFile("src/main/resources/TRX1000.csv").getLines().drop(1).toList

      // Create a class of orders
      case class Order(orderDate: LocalDate, productName: String, expiryDate: LocalDate,
                       quantity: Int, unitPrice: Double, channel: String, paymentMethod: String,
                       priceBefore: Double, discount: Double, priceAfter: Double)

      // A splitter function to parse the line to order object
      def orderSplitter(line: String): Order = {
            val columns = line.split(",")
            val orderDate = formatOrderDate(columns(0))
            val prodName = columns(1)
            val expiryDate = formatExpiryDate(columns(2))
            val quantity = columns(3).toInt
            val unitPrice = columns(4).toDouble
            val channel = columns(5)
            val paymentMethod = columns(6)

            Order(orderDate, prodName, expiryDate, quantity, unitPrice, channel, paymentMethod, 0.0, 0.0, 0.0)
      }

      // Function to format the orderDate to LocalDate
      def formatOrderDate(orderDate: String): LocalDate = {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[XXX][X]")
            LocalDate.parse(orderDate, formatter)
      }

      // Function to format the expiryDate to LocalDate
      def formatExpiryDate(expiryDate: String): LocalDate = {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            LocalDate.parse(expiryDate, formatter)
      }

      //------------------------------------------------------------------Rule 1 ----------------------------------------------------------------------------
      // Function to qualify for expiration date < 30 days
      def isExpiryQualification(orders: Order): Int = {
            val daysRemaining = ChronoUnit.DAYS.between(orders.orderDate.atStartOfDay(), orders.expiryDate.atStartOfDay()).toInt
            daysRemaining
      }

      // Function to calculate the discount for expiryQualification
      def expiryCalculation(daysRemaining: Int): Double = {
            if ((daysRemaining >= 1) && (daysRemaining < 30)) (30 - daysRemaining) / 100.00
            else 0.00
      }

      // Log rule 1
      logger.info("Applying Rule 1: Expiry Qualification")

      //------------------------------------------------------------------Rule 2 -------------------------------------------------------------------------
      // Function to calculate discount if the product is wine or cheese
      def isWineOrCheese(orders: Order): Double = {
            if (orders.productName.contains("Cheese")) 0.1
            else if (orders.productName.contains("Wine")) 0.05
            else 0.0
      }

      // Log rule 2
      logger.info("Applying Rule 2: Wine or Cheese Discount")

      //------------------------------------------------------------------Rule 3 -----------------------------------------------------------------------
      // Function to qualify if the order date is one March 23rd
      def isMarch23(orders: Order): Boolean = {
            orders.orderDate.getMonthValue == 3 && orders.orderDate.getDayOfMonth == 23
      }

      // Function to calculate the discount for the order placed on March 23rd
      def isMarch23Calculation(orderDate: Boolean): Double = {
            if (orderDate) 0.50
            else 0.0
      }

      // Log rule 3
      logger.info("Applying Rule 3: March 23rd Discount")

      //------------------------------------------------------------------Rule 4 -----------------------------------------------------------------------
      // Function to calculate the discount based on the quantity bought
      def isMoreThan5(orders: Order): Double = orders.quantity match {
            case q if q >= 6 && q <= 9 => 0.05
            case q if q >= 10 && q <= 14 => 0.07
            case q if q > 15 => 0.10
            case _ => 0.0 // No discount for quantities less than 6
      }

      // Log rule 4
      logger.info("Applying Rule 4: Quantity Discount")

      ////////////////////////////////////////////////////////////////////New Rules/////////////////////////////////////////////////////////////////////
      //      --------------------------------------------------------------Rule 5 ----------------------------------------------------------------------
      //Function to qualify the qualify if the channel is through our app
      def isAppQualifier(order: Order): Boolean = {
            order.channel == "App"
      }

      //Function to calculate the discount if the channel is through our app
      def isAppCalculator(order: Order, channel: Boolean): Double = {
            if (channel) {
                  // Round up the quantity to the nearest multiple of 5
                  val roundedQuantity = math.ceil(order.quantity.toDouble / 5) * 5
                  roundedQuantity match {
                        case q if q >= 1 && q <= 5 => 0.05
                        case q if q >= 6 && q <= 10 => 0.10
                        case q if q >= 11 && q <= 15 => 0.15
                        case q if q > 15 => 0.20
                        case _ => 0.0 // No discount for quantities less than 1
                  }
            } else 0.0 // No discount if not through the app
      }

      // Log rule 5
      logger.info("Applying Rule 5: App Discount")

      //      ----------------------------------------------------------------Rule 6 ----------------------------------------------------------------------
      //Function to qualify if the payment method is visa
      def isVisaQualification(order: Order): Boolean = {
            order.paymentMethod == "Visa"
      }

      //Function to calculate the discount if the payment method is visa
      def isVisaCalculator(paymentMethod: Boolean): Double = {
            if (paymentMethod) 0.05
            else 0.00
      }

      // Log rule 6
      logger.info("Applying Rule 6: Visa Discount")

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      //Create the discountOrders table on oracle
      try {
            val createTableSQL =
                  """
                    |CREATE TABLE discountOrders (
                    |    orderDate DATE,
                    |    productName VARCHAR2(100),
                    |    expiryDate DATE,
                    |    quantity NUMBER,
                    |    unitPrice NUMBER,
                    |    channel VARCHAR2(100),
                    |    paymentMethod VARCHAR2(100),
                    |    priceBefore NUMBER,
                    |    discount NUMBER,
                    |    priceAfter NUMBER
                    |)
        """.stripMargin

            // Create table
            val statement = connection.createStatement()
            statement.execute(createTableSQL)
            logger.info("Table 'discountOrders' created successfully")

            // Insert data into the table
            val insertSQL =
                  """
                    |INSERT INTO discountOrders (orderDate, productName, expiryDate, quantity, unitPrice, channel, paymentMethod, priceBefore, discount, priceAfter)
                    |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.stripMargin

            val insertStatement = connection.prepareStatement(insertSQL)

            try {
                  val orders = lines.map { orderLine =>
                        val orderObject = orderSplitter(orderLine)
                        val expiryQualificationDiscount = expiryCalculation(isExpiryQualification(orderObject))
                        val wineOrCheeseDiscount = isWineOrCheese(orderObject)
                        val march23Discount = isMarch23Calculation(isMarch23(orderObject))
                        val quantityDiscount = isMoreThan5(orderObject)
                        val appDiscount = isAppCalculator(orderObject, isAppQualifier(orderObject))
                        val visaDiscount = isVisaCalculator(isVisaQualification(orderObject))

                        // Filter discounts greater than 0, sort them in descending order, and take the top 2
                        val discountRules = List(expiryQualificationDiscount, wineOrCheeseDiscount, march23Discount, quantityDiscount, appDiscount, visaDiscount)
                          .filter(_ > 0).sorted.reverse.take(2)

                        // Calculate the average of the top 2 discounts
                        val avgDiscounts = if (discountRules.nonEmpty) {
                              val avg = discountRules.sum / discountRules.size
                              BigDecimal(avg).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
                        } else 0.0

                        // Calculate the price before discount
                        val priceBefore = {
                              val before = orderObject.quantity * orderObject.unitPrice
                              BigDecimal(before).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

                        }

                        // Calculate the price after discount
                        val priceAfter = {
                              val after = priceBefore * (1 - avgDiscounts)
                              BigDecimal(after).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
                        }

                        // Set parameters for the insert statement
                        insertStatement.setDate(1, java.sql.Date.valueOf(orderObject.orderDate))
                        insertStatement.setString(2, orderObject.productName)
                        insertStatement.setDate(3, java.sql.Date.valueOf(orderObject.expiryDate))
                        insertStatement.setInt(4, orderObject.quantity)
                        insertStatement.setDouble(5, orderObject.unitPrice)
                        insertStatement.setString(6, orderObject.channel)
                        insertStatement.setString(7, orderObject.paymentMethod)
                        insertStatement.setDouble(8, priceBefore)
                        insertStatement.setDouble(9, avgDiscounts)
                        insertStatement.setDouble(10, priceAfter)

                        // Execute the insert statement
                        insertStatement.executeUpdate()
                        logger.info(s"Data inserted for product: ${orderObject.productName}, " +
                          s"order date: ${orderObject.orderDate}, " +
                          s"quantity: ${orderObject.quantity}, " +
                          s"unit price: ${orderObject.unitPrice}, " +
                          s"priceBefore: $priceBefore, " +
                          s"discount: $avgDiscounts, " +
                          s"priceAfter: $priceAfter")
                  }
                  logger.info("Data inserted successfully")
            } catch {
                  case e: Exception =>
                        logger.error(s"Error inserting data: ${e.getMessage}")
                        e.printStackTrace()
            } finally {
                  // Close the prepared statement
                  insertStatement.close()
                  connection.close()
            }
      } catch {
            case e: Exception =>
                  logger.error(s"Error creating table: ${e.getMessage}")
                  e.printStackTrace()
      }
}
