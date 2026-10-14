/*******************************************************************************
 ** Example child entity demonstrating parent-child relationships.
 **
 ** This entity shows:
 ** - Foreign key relationship to parent entity
 ** - Conditional inclusion via config toggle
 *******************************************************************************/
package com.kingsrook.qbits.example.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;


@QMetaDataProducingEntity(producePossibleValueSource = true)
public class ExampleChildEntity extends QRecordEntity
{
   public static final String TABLE_NAME = "exampleChildEntity";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = "exampleEntity")
   private Integer exampleEntityId;

   @QField(isRequired = true, maxLength = 100)
   private String name;

   @QField(maxLength = 500)
   private String description;

   @QField
   private Integer sortOrder;

   @QField
   private Boolean isActive = true;

   @QField
   private Instant createDate;

   @QField
   private Instant modifyDate;



   //////////////////////////////////////////////////////////////////////////////
   // Fluent setters                                                           //
   //////////////////////////////////////////////////////////////////////////////

   public ExampleChildEntity withId(Integer id)
   {
      this.id = id;
      return this;
   }


   public ExampleChildEntity withExampleEntityId(Integer exampleEntityId)
   {
      this.exampleEntityId = exampleEntityId;
      return this;
   }


   public ExampleChildEntity withName(String name)
   {
      this.name = name;
      return this;
   }


   public ExampleChildEntity withDescription(String description)
   {
      this.description = description;
      return this;
   }


   public ExampleChildEntity withSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
      return this;
   }


   public ExampleChildEntity withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return this;
   }


   public ExampleChildEntity withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return this;
   }


   public ExampleChildEntity withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return this;
   }



   //////////////////////////////////////////////////////////////////////////////
   // Getters                                                                  //
   //////////////////////////////////////////////////////////////////////////////

   public Integer getId()
   {
      return id;
   }


   public Integer getExampleEntityId()
   {
      return exampleEntityId;
   }


   public String getName()
   {
      return name;
   }


   public String getDescription()
   {
      return description;
   }


   public Integer getSortOrder()
   {
      return sortOrder;
   }


   public Boolean getIsActive()
   {
      return isActive;
   }


   public Instant getCreateDate()
   {
      return createDate;
   }


   public Instant getModifyDate()
   {
      return modifyDate;
   }
}
