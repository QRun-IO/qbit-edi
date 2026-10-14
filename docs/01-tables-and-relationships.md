# Tables and Relationships

Application QBits typically have multiple interconnected tables representing a business domain.

## Entity Design

Use `@QMetaDataProducingEntity` for automatic metadata generation:

```java
@QMetaDataProducingEntity(producePossibleValueSource = true)
public class Order extends QRecordEntity
{
   public static final String TABLE_NAME = "order";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = "customer")
   private Integer customerId;

   @QField(isRequired = true)
   private String status;
}
```

## Relationships

Define foreign keys using `possibleValueSourceName`:

```java
@QField(isRequired = true, possibleValueSourceName = "exampleEntity")
private Integer exampleEntityId;
```

## Table Prefixing

Application QBits support optional table prefixing:

```java
// Without prefix: order, orderLine
// With prefix "sales": sales_order, sales_orderLine

new MyAppQBitProducer()
   .withConfig(new MyAppQBitConfig()
      .withBackendName("rdbms")
      .withTableNamePrefix("sales"))
   .produce(qInstance, "sales-app");
```

The producer applies the prefix to table names. When extending this pattern, also update possible-value-source names, relationship targets, process tables, and navigation references; table renaming alone does not provide complete multi-instance support.
