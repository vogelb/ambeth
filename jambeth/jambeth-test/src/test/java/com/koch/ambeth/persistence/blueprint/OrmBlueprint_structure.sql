CREATE OR REPLACE TYPE "STRING_ARRAY" AS VARRAY(4000) OF VARCHAR2(4000 CHAR);

CREATE TABLE "BL_ENTITY_ANNO"
  (
    "ID"		NUMBER(9,0) NOT NULL,
    "VERSION"	NUMBER(9,0) NOT NULL,
    "TYPE"		VARCHAR2(255 CHAR) NOT NULL,
    "CREATED_ON" DATE,
    "CREATED_BY" VARCHAR2(16 BYTE),
    "UPDATED_ON" DATE,
    "UPDATED_BY" VARCHAR2(16 BYTE),
    CONSTRAINT "BL_ENTITY_ANNO_PK" PRIMARY KEY ("ID") USING INDEX
  );
CREATE SEQUENCE "BL_ENTITY_ANNO_SEQ" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE ;

CREATE TABLE "BL_ENTITY_ANNO_PROPERTY"
  (
    "ID"		NUMBER(9,0) NOT NULL,
    "VERSION"	NUMBER(9,0) NOT NULL,
    "NAME"		VARCHAR2(255 CHAR) NOT NULL,
    "VALUE"		VARCHAR2(255 CHAR) NOT NULL,
    "CREATED_ON" DATE,
    "CREATED_BY" VARCHAR2(16 BYTE),
    "UPDATED_ON" DATE,
    "UPDATED_BY" VARCHAR2(16 BYTE),
    CONSTRAINT "BL_ENTITY_ANNO_PROPERTY_PK" PRIMARY KEY ("ID") USING INDEX
  );
CREATE SEQUENCE "BL_ENTITY_ANNO_PROPERTY_SEQ" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE ;

CREATE TABLE "BL_ENTITY_PROPERTY"
  (
    "ID"		NUMBER(9,0) NOT NULL,
    "VERSION"	NUMBER(9,0) NOT NULL,
    "NAME"		VARCHAR2(255 CHAR) NOT NULL,
    "TYPE"		VARCHAR2(255 CHAR) NOT NULL,
    "ORDER"		NUMBER(9,0) NOT NULL,
    "READONLY"	NUMBER(1,0) NOT NULL,
    "CREATED_ON" DATE,
    "CREATED_BY" VARCHAR2(16 BYTE),
    "UPDATED_ON" DATE,
    "UPDATED_BY" VARCHAR2(16 BYTE),
    CONSTRAINT "BL_ENTITY_PROPERTY_PK" PRIMARY KEY ("ID") USING INDEX
  );
CREATE SEQUENCE "BL_ENTITY_PROPERTY_SEQ" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE ;

CREATE TABLE "BL_ENTITY_TYPE"
  (
    "ID"		NUMBER(9,0) NOT NULL,
    "VERSION"	NUMBER(9,0) NOT NULL,
    "NAME"		VARCHAR2(255 CHAR) NOT NULL,
	"INTERFACES" STRING_ARRAY,
	"SUPERCLASS" VARCHAR2(255 CHAR),
	"IS_CLASS"	NUMBER(1,0) NOT NULL,
    "CREATED_ON" DATE,
    "CREATED_BY" VARCHAR2(16 BYTE),
    "UPDATED_ON" DATE,
    "UPDATED_BY" VARCHAR2(16 BYTE),
    CONSTRAINT "BL_ENTITY_TYPE_PK" PRIMARY KEY ("ID") USING INDEX,
	CONSTRAINT "BL_ENTITY_TYPE_NAME" UNIQUE ("NAME") USING INDEX
  );
CREATE SEQUENCE "BL_ENTITY_TYPE_SEQ" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 10000 CACHE 20 NOORDER NOCYCLE ;

CREATE TABLE "BL_LINK_ANNO_TO_ANNO_PROP"
  (
    "ANNO_ID" NUMBER,
    "ANNO_PROP_ID" NUMBER,
    CONSTRAINT "LINK_ANNO_TO_ANNO_PROP_PK" PRIMARY KEY ("ANNO_ID", "ANNO_PROP_ID") USING INDEX,
    CONSTRAINT "LINK_ANNO_TO_ANNO_PROP_F" FOREIGN KEY ("ANNO_ID") REFERENCES BL_ENTITY_ANNO ("ID") DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT "LINK_ANNO_TO_ANNO_PROP_TO" FOREIGN KEY ("ANNO_PROP_ID") REFERENCES BL_ENTITY_ANNO_PROPERTY ("ID") DEFERRABLE INITIALLY DEFERRED
  );
  
  CREATE TABLE "BL_LINK_ENTITY_PROP_TO_ANNO"
  (
    "ENTITY_PROP_ID"	NUMBER,
    "ANNO_ID"			NUMBER,
    CONSTRAINT "LINK_ENTITY_PROP_TO_ANNO_PK" PRIMARY KEY ("ENTITY_PROP_ID", "ANNO_ID") USING INDEX,
    CONSTRAINT "LINK_ENTITY_PROP_TO_ANNO_F" FOREIGN KEY ("ENTITY_PROP_ID") REFERENCES BL_ENTITY_PROPERTY ("ID") DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT "LINK_ENTITY_PROP_TO_ANNO_TO" FOREIGN KEY ("ANNO_ID") REFERENCES BL_ENTITY_ANNO ("ID") DEFERRABLE INITIALLY DEFERRED
  );
  
  CREATE TABLE "BL_LINK_ENTITY_TO_ANNO"
  (
    "ENTITY_ID" NUMBER,
    "ANNO_ID"	NUMBER,
    CONSTRAINT "LINK_ENTITY_TO_ANNO_PK" PRIMARY KEY ("ENTITY_ID", "ANNO_ID") USING INDEX,
    CONSTRAINT "LINK_ENTITY_TO_ANNO_F" FOREIGN KEY ("ENTITY_ID") REFERENCES BL_ENTITY_TYPE ("ID") DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT "LINK_ENTITY_TO_ANNO_TO" FOREIGN KEY ("ANNO_ID") REFERENCES BL_ENTITY_ANNO ("ID") DEFERRABLE INITIALLY DEFERRED
  );
  
  CREATE TABLE "BL_LINK_ENTITY_TO_ENTITY_PROP"
  (
    "ENTITY_ID"			NUMBER,
    "ENTITY_PROP_ID"	NUMBER,
    CONSTRAINT "LINK_ENTITY_TO_ENTITY_PROP_PK" PRIMARY KEY ("ENTITY_ID", "ENTITY_PROP_ID") USING INDEX,
    CONSTRAINT "LINK_ENTITY_TO_ENTITY_PROP_F" FOREIGN KEY ("ENTITY_ID") REFERENCES BL_ENTITY_TYPE ("ID") DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT "LINK_ENTITY_TO_ENTITY_PROP_TO" FOREIGN KEY ("ENTITY_PROP_ID") REFERENCES BL_ENTITY_PROPERTY ("ID") DEFERRABLE INITIALLY DEFERRED
  );
