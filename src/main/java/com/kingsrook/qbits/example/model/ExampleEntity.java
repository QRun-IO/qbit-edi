/*******************************************************************************
 ** Example entity for an Application QBit.
 **
 ** This entity demonstrates:
 ** - Standard QQQ entity patterns
 ** - PossibleValueSource generation
 ** - Fields with various types and constraints
 *******************************************************************************/
package com.kingsrook.qbits.example.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;


@QMetaDataProducingEntity(producePossibleValueSource = true)
public class ExampleEntity extends QRecordEntity
{
   public static final String TABLE_NAME = "exampleEntity";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 100)
   private String name;

   @QField(maxLength = 500)
   private String description;

   @QField(label = "Status")
   private String status;

   @QField
   private Boolean isActive = true;

   @QField
   private Instant createDate;

   @QField
   private Instant modifyDate;



   //////////////////////////////////////////////////////////////////////////////
   // Fluent setters                                                           //
   //////////////////////////////////////////////////////////////////////////////

   public ExampleEntity withId(Integer id)
   {
      this.id = id;
      return this;
   }


   public ExampleEntity withName(String name)
   {
      this.name = name;
      return this;
   }


   public ExampleEntity withDescription(String description)
   {
      this.description = description;
      return this;
   }


   public ExampleEntity withStatus(String status)
   {
      this.status = status;
      return this;
   }


   public ExampleEntity withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return this;
   }


   public ExampleEntity withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return this;
   }


   public ExampleEntity withModifyDate(Instant modifyDate)
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


   public String getName()
   {
      return name;
   }


   public String getDescription()
   {
      return description;
   }


   public String getStatus()
   {
      return status;
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
