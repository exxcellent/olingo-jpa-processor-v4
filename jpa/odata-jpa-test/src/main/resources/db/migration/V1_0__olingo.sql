--CREATE ALIAS IS_PRIME FOR "org.apache.olingo.jpa.processor.core.test_udf.UserDefinedFunctions.isPrime";
SET schema "OLINGO";
    
CREATE TABLE "org.apache.olingo.jpa::BusinessPartner" (

	"ID" VARCHAR(32) NOT NULL ,
	"ETag" BIGINT NOT NULL,
	"Type" VARCHAR(2) NOT NULL,
	"CustomString1" VARCHAR(250),
	"CustomString2" VARCHAR(250),
	"CustomNum1" DECIMAL(16,5),
	"CustomNum2"  DECIMAL(31,0),
	"NameLine1" VARCHAR(250),
	"NameLine2" VARCHAR(250),
	"BirthDay" DATE,
	"Address.StreetName" VARCHAR(200),
    "Address.StreetNumber" VARCHAR(60),
    "Address.PostOfficeBox" VARCHAR(60),
    "Address.City" VARCHAR(100),
    "Address.PostalCode" VARCHAR(60),
    "Address.RegionCodePublisher" VARCHAR(10) NOT NULL,
	"Address.RegionCodeID" VARCHAR(10) NOT NULL,
    "Address.Region" VARCHAR(100),
    "Address.Country" VARCHAR(100),
    "Telecom.Phone" VARCHAR(100),
    "Telecom.Mobile" VARCHAR(100),
    "Telecom.Fax" VARCHAR(100),
    "Telecom.Email" VARCHAR(100),
	"CreatedBy" VARCHAR(32) NOT NULL,
	"CreatedAt" TIMESTAMP,   
	"UpdatedBy" VARCHAR(32),
	"UpdatedAt" TIMESTAMP,
    "Country" VARCHAR(4),
	 PRIMARY KEY ("ID")
);
     
-- organizations
insert into "org.apache.olingo.jpa::BusinessPartner" values ('1', 0, '2', '','',6000.5,null,'First Org.','',null,'Test Road', '23','', 'Test City','94321','ISO', '3166-2','US-CA', 'USA', '', '','','', '99','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('2', 0, '2', '','',null,null,'Second Org.','',null,'Test Road', '45','', 'Test City','76321','ISO', '3166-2','US-TX', 'USA', '', '','','', '97','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('3', 0, '2', '','',null,null,'Third Org.','',null,'Test Road', '223','', 'Test City','94322','ISO', '3166-2','US-CA', 'USA', '', '','','', '99','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('4', 0, '2', '','',null,null,'Fourth Org.','',null,'Test Road', '56','', 'Test City','84321','ISO', '3166-2','US-UT', 'USA', '', '','','', '98','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('5', 0, '2', '','',null,null,'Fifth Org.','',null,'Test Road', '35','', 'Test City','59321','ISO', '3166-2','US-MT', 'USA', '', '','','', '99','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('6', 0, '2', '','',null,null,'Sixth Org.','',null,'Test Road', '7856','', 'Test City','94324','ISO', '3166-2','US-CA', 'USA', '', '','','', '99','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('7', 0, '2', '','',null,null,'Seventh Org.','',null,'Test Road', '4','', 'Test City','29321','ISO', '3166-2','US-SC', 'USA', '', '','','', '99','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('8', 0, '2', '','',null,null,'Eighth Org.','',null,'Test Road', '453','', 'Test City','29221','ISO', '3166-2','US-SC', 'USA', '', '','','', '99','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('9', 0, '2', '','',null,null,'Ninth Org.','',null,'Test Road', '93','', 'Test City','55021','ISO', '3166-2','US-MN', 'USA', '', '','','', '99','2016-01-20 09:21:23', '', null, 'USA');
insert into "org.apache.olingo.jpa::BusinessPartner" values ('10', 0, '2', '','',null,null,'Tenth Org.','',null,'Test Road', '12','', 'Test City','03921','ISO', '3166-2','US-ME', 'USA', '', '','','', '99','2016-01-20 09:21:23', '', null, 'DEU');
-- persons
insert into "org.apache.olingo.jpa::BusinessPartner" values ('99', 0, '1', '','',null,null,'Max','Mustermann',null,'Test Starße', '12','', 'Teststadt','10115','ISO', '3166-2','DE-BE', 'DEU', '', '','','', '99','2016-01-20 09:21:23', null, null, 'DEU'); 
insert into "org.apache.olingo.jpa::BusinessPartner" values ('98', 0, '1', '','',null,null,'John','Doe',null,'Test Road', '55','', 'Test City','76321','ISO', '3166-2','US-TX', 'USA', '', '','','', '99','2016-01-20 09:21:23', null, null, 'DEU'); 
insert into "org.apache.olingo.jpa::BusinessPartner" values ('97', 0, '1', '','',null,null,'Urs','Müller',null,'Test Straße', '23','', 'Test Dorf','4123','ISO', '3166-2','CH-BL', 'CHE', '', '','','', '99','2016-07-20 09:21:23', null, null, 'CHE'); 

-- used for @ElementCollection test
CREATE TABLE "org.apache.olingo.jpa::Phone" (
	"PartnerID" VARCHAR(32) NOT NULL ,
	"PhoneNumber" VARCHAR(128)
);

insert into "org.apache.olingo.jpa::Phone" values ('99','+49 1235 8888'); 	
insert into "org.apache.olingo.jpa::Phone" values ('99','01235 8888'); 	
insert into "org.apache.olingo.jpa::Phone" values ('98','+42 01235 8888-44444'); 	
insert into "org.apache.olingo.jpa::Phone" values ('98','+42 01265 8888-44444/15'); 	
insert into "org.apache.olingo.jpa::Phone" values ('97','+42/1234/987654321'); 	

CREATE TABLE "org.apache.olingo.jpa::PersonImage" (
	"PID" VARCHAR(32) NOT NULL , -- standard behaviour to navigate to a Person
	"NOT_MAPPED_PID" VARCHAR(32) NOT NULL , -- used to join in JPA, but without explicit attribute in model
	PersonWithDefaultIdMapping_ID VARCHAR(32) NOT NULL , -- special JPA case to auto build a join column name for navigation without further mapping informations; do NOT Wrap the column name with ""!!!
	"Image" BLOB,
	"CreatedBy" VARCHAR(32) NOT NULL ,
	"CreatedAt" TIMESTAMP,   
	"UpdatedBy" VARCHAR(32),
	"UpdatedAt" TIMESTAMP,
	 PRIMARY KEY ("PID")
);
 
insert into "org.apache.olingo.jpa::PersonImage" values ('99', '98', '97', null, '99', '2016-01-20 09:21:23', null, null); 	

CREATE TABLE "org.apache.olingo.jpa::OrganizationImage" (
	"ID" VARCHAR(32) NOT NULL ,
	"Image" BLOB,
	"MimeType"  VARCHAR(100),
	"CreatedBy" VARCHAR(32) NOT NULL ,
	"CreatedAt" TIMESTAMP,   
	"UpdatedBy" VARCHAR(32),
	"UpdatedAt" TIMESTAMP,
	"ThumbnailUrl" VARCHAR(250),
	 PRIMARY KEY ("ID"))
;

insert into "org.apache.olingo.jpa::OrganizationImage" values ('9',null,'image/svg+xml','99','2016-01-20 09:21:23', null, null, 'http://www.anywhere.org/image.jpg'); 

CREATE TABLE "org.apache.olingo.jpa::BusinessPartnerRole" ( 
	"BusinessPartnerID" VARCHAR(32) NOT NULL ,
	"BusinessPartnerRole" VARCHAR(10) NOT NULL, 
     PRIMARY KEY ("BusinessPartnerID","BusinessPartnerRole"))
;

insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('1',  'A');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('3',  'A');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('3',  'B');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('3',  'C');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('2',  'A');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('2',  'C');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('7',  'C');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('98',  'X');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('99',  'X');
insert into "org.apache.olingo.jpa::BusinessPartnerRole" values ('99',  'Z');

CREATE TABLE "org.apache.olingo.jpa::CountryDescription" ( 
	"ISOCode" VARCHAR(4) NOT NULL ,
	"LanguageISO" VARCHAR(4) NOT NULL ,
	"Name" VARCHAR(100) NOT NULL, 
     PRIMARY KEY ("ISOCode","LanguageISO"))
;
 
insert into "org.apache.olingo.jpa::CountryDescription" values( 'DEU','de','Deutschland');    
insert into "org.apache.olingo.jpa::CountryDescription" values( 'USA','de','Vereinigte Staaten von Amerika');   
insert into "org.apache.olingo.jpa::CountryDescription" values( 'DEU','en','Germany');
insert into "org.apache.olingo.jpa::CountryDescription" values( 'USA','en','United States of America');
insert into "org.apache.olingo.jpa::CountryDescription" values( 'BEL','de','Belgien');
insert into "org.apache.olingo.jpa::CountryDescription" values( 'BEL','en','Belgium');
insert into "org.apache.olingo.jpa::CountryDescription" values( 'CHE','de','Schweiz');
insert into "org.apache.olingo.jpa::CountryDescription" values( 'CHE','en','Switzerland');

CREATE TABLE "org.apache.olingo.jpa::AdministrativeDivisionDescription"(
	"CodePublisher" VARCHAR(10) NOT NULL,
	"CodeID" VARCHAR(10) NOT NULL,
	"DivisionCode" VARCHAR(10) NOT NULL,
	"LanguageISO" VARCHAR(4) NOT NULL ,
	"Name" VARCHAR(100) NOT NULL, 
     PRIMARY KEY ("CodePublisher", "CodeID", "DivisionCode","LanguageISO"))
; 

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS1','CH0','de','Schweiz');     
     
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-1','DEU','de','Deutschland');    
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-1','USA','de','Vereinigte Staaten von Amerika');   
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-1','DEU','en','Germany');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-1','USA','en','United States of America');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-1','BEL','de','Belgien');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-1','BEL','en','Belgium');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-1','CHE','de','Schweiz');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-1','CHE','en','Switzerland');     
     
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-BW','de','Baden-Württemberg');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-BY','de','Bayern Bayern');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-BE','de','Berlin');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-BB','de','Brandenburg');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-HB','de','Bremen');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-HH','de','Hamburg');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-HE','de','Hessen');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-MV','de','Mecklenburg-Vorpommern');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-NI','de','Niedersachsen');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-NW','de','Nordrhein-Westfalen');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-RP','de','Rheinland-Pfalz');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-SL','de','Saarland');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-SN','de','Sachsen');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-ST','de','Sachsen-Anhalt');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-SH','de','Schleswig-Holstein');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-TH','de','Thüringen');  

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-BW','en','Baden-Württemberg');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-BY','en','Bavaria');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-BE','en','Berlin');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-BB','en','Brandenburg');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-HB','en','Bremen');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-HH','en','Hamburg');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-HE','en','Hesse');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-MV','en','Mecklenburg-Western Pomerania');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-NI','en','Lower Saxony');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-NW','en','North Rhine-Westphalia');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-RP','en','Rhineland-Palatinate');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-SL','en','Saarland');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-SN','en','Saxony');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-ST','en','Saxony-Anhalt');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-SH','en','Schleswig-Holstein');  
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','DE-TH','en','Thuringia');  


insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-AG','de','Aargau'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-AR','de','Appenzell Ausserrhoden'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-AI','de','Appenzell Innerrhoden'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-BL','de','Basel-Landschaft'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-BS','de','Basel-Stadt'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-BE','de','Bern'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-FR','de','Freiburg'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-GE','de','Genf'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-GL','de','Glarus'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-GR','de','Graubünden'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-JU','de','Jura'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-LU','de','Luzern'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-NE','de','Neuenburg'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-NW','de','Nidwalden'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-OW','de','Obwalden'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-SH','de','Schaffhausen'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-SZ','de','Schwyz'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-SO','de','Solothurn'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-SG','de','St. Gallen'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-TI','de','Tessin'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-TG','de','Thurgau'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-UR','de','Uri'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-VD','de','Waadt'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-VS','de','Wallis'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-ZG','de','Zug'); 
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','CH-ZH','de','Zürich'); 

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-AL','de','Alabama');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-AK','de','Alaska');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-AZ','de','Arizona');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-AR','de','Arkansas');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-CO','de','Colorado');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-CT','de','Connecticut');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-DE','de','Delaware');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-DC','de','District of Columbia');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-FL','de','Florida');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-GA','de','Georgia');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-HI','de','Hawaii');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-ID','de','Idaho');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-IL','de','Illinois');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-IN','de','Indiana');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-IA','de','Iowa');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-CA','de','Kalifornien');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-KS','de','Kansas');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-KY','de','Kentucky');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-LA','de','Louisiana');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-ME','de','Maine');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MD','de','Maryland');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MA','de','Massachusetts');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MI','de','Michigan');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MN','de','Minnesota');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MS','de','Mississippi');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MO','de','Missouri');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MT','de','Montana');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NE','de','Nebraska');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NV','de','Nevada');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NH','de','New Hampshire');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NJ','de','New Jersey');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NM','de','New Mexico');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NY','de','New York');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NC','de','North Carolina');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-ND','de','North Dakota');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-OH','de','Ohio');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-OK','de','Oklahoma');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-OR','de','Oregon');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-PA','de','Pennsylvania');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-RI','de','Rhode Island');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-SC','de','South Carolina');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-SD','de','South Dakota');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-TN','de','Tennessee');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-TX','de','Texas');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-UT','de','Utah');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-VT','de','Vermont');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-VA','de','Virginia');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-WA','de','Washington');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-WV','de','West Virginia');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-WI','de','Wisconsin');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-WY','de','Wyoming');

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-AL','en','Alabama');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-AK','en','Alaska');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-AZ','en','Arizona');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-AR','en','Arkansas');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-CA','en','California');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-CO','en','Colorado');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-CT','en','Connecticut');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-DE','en','Delaware');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-FL','en','Florida');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-GA','en','Georgia');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-HI','en','Hawaii');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-ID','en','Idaho');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-IL','en','Illinois');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-IN','en','Indiana');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-IA','en','Iowa');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-KS','en','Kansas');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-KY','en','Kentucky');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-LA','en','Louisiana');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-ME','en','Maine');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MD','en','Maryland');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MA','en','Massachusetts');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MI','en','Michigan');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MN','en','Minnesota');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MS','en','Mississippi');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MO','en','Missouri');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-MT','en','Montana');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NE','en','Nebraska');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NV','en','Nevada');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NH','en','New Hampshire');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NJ','en','New Jersey');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NM','en','New Mexico');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NY','en','New York');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-NC','en','North Carolina');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-ND','en','North Dakota');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-OH','en','Ohio');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-OK','en','Oklahoma');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-OR','en','Oregon');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-PA','en','Pennsylvania');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-RI','en','Rhode Island');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-SC','en','South Carolina');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-SD','en','South Dakota');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-TN','en','Tennessee');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-TX','en','Texas');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-UT','en','Utah');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-VT','en','Vermont');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-VA','en','Virginia');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-WA','en','Washington');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-WV','en','West Virginia');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-WI','en','Wisconsin');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-WY','en','Wyoming');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'ISO', '3166-2','US-DC','en','District of Columbia');
     

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS1','BE1','de','Region Brüssel-Hauptstadt');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS1','BE2','de','Flämische Region');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS1','BE3','de','Wallonische Region');

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE10','de','Region Brüssel-Hauptstadt');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE21','de','Provinz Antwerpen');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE22','de','Provinz Limburg');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE23','de','Provinz Ostflandern');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE24','de','Provinz Flämisch-Brabant');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE25','de','Provinz Westflandern');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE31','de','Provinz Wallonisch-Brabant');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE32','de','Provinz Hennegau');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE33','de','Provinz Lüttich');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE34','de','Provinz Luxemburg');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE35','de','Provinz Namur');

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE100','de','Bezirk Brüssel-Hauptstadt');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE211','de','Bezirk Antwerpen');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE212','de','Bezirk Mechelen');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE213','de','Bezirk Turnhout');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE221','de','Bezirk Hasselt');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE222','de','Bezirk Maaseik');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE223','de','Bezirk Tongeren');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE231','de','Bezirk Aalst');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE232','de','Bezirk Dendermonde');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE233','de','Bezirk Eeklo');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE234','de','Bezirk Gent');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE235','de','Bezirk Oudenaarde');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE236','de','Bezirk Sint-Niklaas');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE241','de','Bezirk Halle-Vilvoorde');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE242','de','Bezirk Löwen');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE251','de','Bezirk Brügge');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE252','de','Bezirk Diksmuide');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE253','de','Bezirk Ypern');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE254','de','Bezirk Kortrijk');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE255','de','Bezirk Ostende');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE256','de','Bezirk Roeselare');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE257','de','Bezirk Tielt');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE258','de','Bezirk Veurne');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE310','de','Bezirk Nivelles');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE321','de','Bezirk Ath');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE322','de','Bezirk Charleroi');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE323','de','Bezirk Mons');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE324','de','Bezirk Mouscron');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE325','de','Bezirk Soignies');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE326','de','Bezirk Thuin');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE327','de','Bezirk Tournai');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE331','de','Bezirk Huy');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE332','de','Bezirk Lüttich');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE334','de','Bezirk Waremme');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE335','de','Bezirk Verviers – frz. Sprachgebiet');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE336','de','Bezirk Verviers – deu. Sprachgebiet');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE341','de','Bezirk Arlon');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE342','de','Bezirk Bastogne');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE343','de','Bezirk Marche-en-Famenne');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE344','de','Bezirk Neufchâteau');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE345','de','Bezirk Virton');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE351','de','Bezirk Dinant');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE352','de','Bezirk Namur');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE353','de','Bezirk Philippeville');

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS1','BE1','en','Brussels-Capital Region');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS1','BE2','en','Flemish Region');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS1','BE3','en','Walloon Region');

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE10','en','Brussels-Capital Region');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE21','en','Antwerp');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE22','en','Limburg');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE23','en','East Flanders');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE24','en','Flemish Brabant');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE25','en','West Flanders');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE31','en','Walloon Brabant');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE32','en','Hainaut');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE33','en','Liège');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE34','en','Luxembourg (Belgium)');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS2','BE35','en','Namur');

insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE100','en','Arrondissement of Brussels-Capital');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE211','en','Arrondissement of Antwerp');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE212','en','Arrondissement of Mechelen');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE213','en','Arrondissement of Turnhout');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE221','en','Arrondissement of Hasselt');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE222','en','Arrondissement of Maaseik');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE223','en','Arrondissement of Tongeren');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE231','en','Arrondissement of Aalst');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE232','en','Arrondissement of Dendermonde');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE233','en','Arrondissement of Eeklo');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE234','en','Arrondissement of Ghent');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE235','en','Arrondissement of Oudenaarde');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE236','en','Arrondissement of Sint-Niklaas');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE241','en','Arrondissement of Halle-Vilvoorde');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE242','en','Arrondissement of Leuven');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE251','en','Arrondissement of Bruges');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE252','en','Arrondissement of Diksmuide');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE253','en','Arrondissement of Ypres');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE254','en','Arrondissement of Kortrijk');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE255','en','Arrondissement of Ostend');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE256','en','Arrondissement of Roeselare');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE257','en','Arrondissement of Tielt');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE258','en','Arrondissement of Veurne');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE310','en','Arrondissement of Nivelles');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE321','en','Arrondissement of Ath');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE322','en','Arrondissement of Charleroi');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE323','en','Arrondissement of Mons');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE324','en','Arrondissement of Mouscron');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE325','en','Arrondissement of Soignies');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE326','en','Arrondissement of Thuin');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE327','en','Arrondissement of Tournai');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE331','en','Arrondissement of Huy');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE332','en','Arrondissement of Liège');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE334','en','Arrondissement of Waremme');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE335','en','Arrondissement of Verviers, municipalities of the French Community');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE336','en','Arrondissement of Verviers,municipalities of the German Community');	
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE341','en','Arrondissement of Arlon');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE342','en','Arrondissement of Bastogne');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE343','en','Arrondissement of Marche-en-Famenne');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE344','en','Arrondissement of Neufchâteau');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE345','en','Arrondissement of Virton');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE351','en','Arrondissement of Dinant');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE352','en','Arrondissement of Namur');
insert into "org.apache.olingo.jpa::AdministrativeDivisionDescription" values( 'Eurostat','NUTS3','BE353','en','Arrondissement of Philippeville');

-- helpertable+ view to simulate a join table for m:n relationship between BusinessPartner and AdministrativeDivisionDescription
CREATE TABLE "org.apache.olingo.jpa::BPADDJoinTable"(
	"BusinessPartnerID" VARCHAR(32) NOT NULL ,
	"CodePublisher" VARCHAR(10) NOT NULL,
	"CodeID" VARCHAR(10) NOT NULL,
	"DivisionCode" VARCHAR(10) NOT NULL,
	"LanguageISO" VARCHAR(4) NOT NULL)
;

INSERT INTO "org.apache.olingo.jpa::BPADDJoinTable" 
	SELECT BP."ID" AS "BusinessPartnerID", AD."CodePublisher", AD."CodeID", AD."DivisionCode", AD."LanguageISO"
	FROM "org.apache.olingo.jpa::AdministrativeDivisionDescription" AS AD
	JOIN "org.apache.olingo.jpa::BusinessPartner" AS BP ON BP."Address.Region" = AD."DivisionCode"
; 


CREATE TABLE "org.apache.olingo.jpa::AdministrativeDivision"(
	"CodePublisher" VARCHAR(10) NOT NULL,
	"CodeID" VARCHAR(10) NOT NULL,
	"DivisionCode" VARCHAR(10) NOT NULL,
	"CountryISOCode" VARCHAR(4) NOT NULL ,
	"ParentCodeID" VARCHAR(10),
	"ParentDivisionCode" VARCHAR(10),
	"AlternativeCode" VARCHAR(10),
	"Area" int,  --DECIMAL(31,0),
	"Population" BIGINT,
	PRIMARY KEY ("CodePublisher", "CodeID", "DivisionCode"))
;
	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-1','BEL','BEL',null,null,null,0,0);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-1','DEU','DEU',null,null,null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-1','USA','USA',null,null,null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-1','CHE','CHE',null,null,null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-AG','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-AR','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-AI','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-BL','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-BS','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-BE','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-FR','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-GE','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-GL','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-GR','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-JU','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-LU','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-NE','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-NW','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-OW','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-SH','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-SZ','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-SO','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-SG','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-TI','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-TG','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-UR','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-VD','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-VS','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-ZG','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','CH-ZH','CHE','3166-1','CHE',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-BRU','BEL','3166-1','BEL',null,0,0);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-VLG','BEL','3166-1','BEL',null,0,0);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-WAL','BEL','3166-1','BEL',null,0,0);		
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-VAN','BEL','3166-2','BE-VLG',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-VLI','BEL','3166-2','BE-VLG',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-VOV','BEL','3166-2','BE-VLG',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-VBR','BEL','3166-2','BE-VLG',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-VWV','BEL','3166-2','BE-VLG',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-WBR','BEL','3166-2','BE-WAL',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-WHT','BEL','3166-2','BE-WAL',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-WLG','BEL','3166-2','BE-WAL',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-WLX','BEL','3166-2','BE-WAL',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','BE-WNA','BEL','3166-2','BE-WAL',null,0,0);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-BW','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-BY','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-BE','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-BB','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-HB','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-HH','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-HE','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-MV','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-NI','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-NW','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-RP','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-SL','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-SN','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-ST','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-SH','DEU', '3166-1','DEU',null,0,0);  
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2', 'DE-TH','DEU', '3166-1','DEU',null,0,0); 
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-AL','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-AK','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-AZ','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-AR','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-CA','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-CO','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-CT','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-DE','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-FL','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-GA','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-HI','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-ID','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-IL','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-IN','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-IA','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-KS','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-KY','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-LA','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-ME','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-MD','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-MA','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-MI','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-MN','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-MS','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-MO','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-MT','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-NE','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-NV','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-NH','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-NJ','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-NM','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-NY','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-NC','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-ND','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-OH','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-OK','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-OR','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-PA','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-RI','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-SC','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-SD','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-TN','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-TX','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-UT','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-VT','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-VA','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-WA','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-WV','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-WI','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-WY','USA','3166-1','USA',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'ISO', '3166-2','US-DC','USA','3166-1','USA',null,0,0);
--Eurostat 
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS1','BE1', 'BEL',null,null,null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS1','BE2', 'BEL',null,null,null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS1','BE3', 'BEL',null,null,null,0,0);

insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE10','BEL','NUTS1','BE1',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE21','BEL','NUTS1','BE2',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE22','BEL','NUTS1','BE2',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE23','BEL','NUTS1','BE2',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE24','BEL','NUTS1','BE2',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE25','BEL','NUTS1','BE2',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE31','BEL','NUTS1','BE3',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE32','BEL','NUTS1','BE3',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE33','BEL','NUTS1','BE3',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE34','BEL','NUTS1','BE3',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS2','BE35','BEL','NUTS1','BE3',null,0,0);

insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE100','BEL','NUTS2','BE10',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE211','BEL','NUTS2','BE21',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE212','BEL','NUTS2','BE21',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE213','BEL','NUTS2','BE21',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE221','BEL','NUTS2','BE22',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE222','BEL','NUTS2','BE22',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE223','BEL','NUTS2','BE22',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE231','BEL','NUTS2','BE23',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE232','BEL','NUTS2','BE23',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE233','BEL','NUTS2','BE23',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE234','BEL','NUTS2','BE23',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE235','BEL','NUTS2','BE23',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE236','BEL','NUTS2','BE23',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE241','BEL','NUTS2','BE24',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE242','BEL','NUTS2','BE24',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE251','BEL','NUTS2','BE25',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE252','BEL','NUTS2','BE25',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE253','BEL','NUTS2','BE25',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE254','BEL','NUTS2','BE25',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE255','BEL','NUTS2','BE25',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE256','BEL','NUTS2','BE25',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE257','BEL','NUTS2','BE25',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE258','BEL','NUTS2','BE25',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE310','BEL','NUTS2','BE31',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE321','BEL','NUTS2','BE32',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE322','BEL','NUTS2','BE32',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE323','BEL','NUTS2','BE32',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE324','BEL','NUTS2','BE32',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE325','BEL','NUTS2','BE32',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE326','BEL','NUTS2','BE32',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE327','BEL','NUTS2','BE32',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE331','BEL','NUTS2','BE33',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE332','BEL','NUTS2','BE33',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE334','BEL','NUTS2','BE33',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE335','BEL','NUTS2','BE33',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE336','BEL','NUTS2','BE33',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE341','BEL','NUTS2','BE34',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE342','BEL','NUTS2','BE34',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE343','BEL','NUTS2','BE34',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE344','BEL','NUTS2','BE34',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE345','BEL','NUTS2','BE34',null,0,0);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE351','BEL','NUTS2','BE35',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE352','BEL','NUTS2','BE35',null,0,0);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat','NUTS3','BE353','BEL','NUTS2','BE35',null,0,0);

insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '33011','BEL','NUTS3','BE253',null,130610415,35098);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '33016','BEL','NUTS3','BE253',null,3578335,1037);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '33021','BEL','NUTS3','BE253',null,119330610,19968);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '33029','BEL','NUTS3','BE253',null,43612479,18456);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '33037','BEL','NUTS3','BE253',null,67573324,12400);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '33039','BEL','NUTS3','BE253',null,94235304,7888);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '33040','BEL','NUTS3','BE253',null,52529046,8144);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '33041','BEL','NUTS3','BE253',null,38148241,3625);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31003','BEL','NUTS3','BE251',null,71675809,15493);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31004','BEL','NUTS3','BE251',null,17411180,20028);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31005','BEL','NUTS3','BE251',null,138402202,118335);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31006','BEL','NUTS3','BE251',null,89520475,10885);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31012','BEL','NUTS3','BE251',null,53764838,13861);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31022','BEL','NUTS3','BE251',null,79645460,23133);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31033','BEL','NUTS3','BE251',null,45232765,20371);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31040','BEL','NUTS3','BE251',null,60335913,22424);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31042','BEL','NUTS3','BE251',null,48862499,2720);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '31043','BEL','NUTS3','BE251',null,56443228,33485);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '32003','BEL','NUTS3','BE252',null,149401818,16564);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '32006','BEL','NUTS3','BE252',null,55893936,9995);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '32010','BEL','NUTS3','BE252',null,39185258,8712);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '32011','BEL','NUTS3','BE252',null,54999796,12357);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '32030','BEL','NUTS3','BE252',null,62938759,3282);
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34002','BEL','NUTS3','BE254',null,41788652,14580);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34003','BEL','NUTS3','BE254',null,21748127,9803);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34009','BEL','NUTS3','BE254',null,16815679,11687);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34013','BEL','NUTS3','BE254',null,29140007,27476);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34022','BEL','NUTS3','BE254',null,80020386,75577);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34023','BEL','NUTS3','BE254',null,10008346,13113);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34025','BEL','NUTS3','BE254',null,13150180,5756);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34027','BEL','NUTS3','BE254',null,33070836,33099);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34040','BEL','NUTS3','BE254',null,44343752,37385);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34041','BEL','NUTS3','BE254',null,38761463,31345);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34042','BEL','NUTS3','BE254',null,63242438,24301);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '34043','BEL','NUTS3','BE254',null,10776650,2155);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '35002','BEL','NUTS3','BE255',null,13079087,17333);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '35005','BEL','NUTS3','BE255',null,42254065,11771);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '35006','BEL','NUTS3','BE255',null,45334695,13972);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '35011','BEL','NUTS3','BE255',null,75653353,19312);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '35013','BEL','NUTS3','BE255',null,37723883,70813);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '35014','BEL','NUTS3','BE255',null,35383003,9231);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '35029','BEL','NUTS3','BE255',null,42169175,12611);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '36006','BEL','NUTS3','BE256',null,37836247,10079);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '36007','BEL','NUTS3','BE256',null,16157265,10748);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '36008','BEL','NUTS3','BE256',null,25483137,27449);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '36010','BEL','NUTS3','BE256',null,24758200,9519);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '36011','BEL','NUTS3','BE256',null,25931415,8625);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '36012','BEL','NUTS3','BE256',null,35341058,10964);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '36015','BEL','NUTS3','BE256',null,59793935,60707);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '36019','BEL','NUTS3','BE256',null,46240034,11196);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37002','BEL','NUTS3','BE257',null,25935511,8376);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37007','BEL','NUTS3','BE257',null,29348057,11039);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37010','BEL','NUTS3','BE257',null,16621841,7715);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37011','BEL','NUTS3','BE257',null,34421545,6798);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37012','BEL','NUTS3','BE257',null,30201523,5266);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37015','BEL','NUTS3','BE257',null,68504357,20110);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37017','BEL','NUTS3','BE257',null,21760376,9441);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37018','BEL','NUTS3','BE257',null,68420735,14283);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '37020','BEL','NUTS3','BE257',null,34576094,9072);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '38002','BEL','NUTS3','BE258',null,80009790,5007);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '38008','BEL','NUTS3','BE258',null,23896896,10854);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '38014','BEL','NUTS3','BE258',null,43959624,22202);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '38016','BEL','NUTS3','BE258',null,31004430,11434);	
insert into "org.apache.olingo.jpa::AdministrativeDivision" values( 'Eurostat', 'LAU2', '38025','BEL','NUTS3','BE258',null,96339703,11509);

CREATE TABLE "org.apache.olingo.jpa::DummyToBeIgnored" (
	"ID" VARCHAR(32) NOT NULL ,
	 PRIMARY KEY ("ID"))
;

-- additional table only used to test datatype support/conversion between JPA and oData
CREATE TABLE "org.apache.olingo.jpa::DatatypeConversionEntity" (
	"ID" BIGINT NOT NULL ,
	"ADate1" DATE,
	"ADate2" DATE,
	"ADate3" DATE,
	"ATimestamp1" TIMESTAMP,
	"ATimestamp2" TIMESTAMP,
	"ADecimal" DECIMAL(16,5),
	"AUrlString" CLOB,
	"ADuration" INTEGER,
	"AYear" INTEGER,
	"AStringMappedEnum" VARCHAR(255),
	"AOrdinalMappedEnum" INTEGER,
	"AOtherPackageEnum" VARCHAR(255),
	"UUID" VARCHAR(128),
	"ABoolean" SMALLINT NOT NULL,
	 PRIMARY KEY ("ID"))
;

insert into "org.apache.olingo.jpa::DatatypeConversionEntity" values( 1, '0610-01-01', null, null, '2010-01-01 09:21:00', '2016-01-20 09:21:23', 12.34, 'http://www.anywhere.org/image.jpg', 123, 1900, 'CE', 0, 'Two', '7f905a0b-bb6e-11e3-9e8f-000000000000', 1);
	 
-- additional table only used to test relationship scenarios
CREATE TABLE "org.apache.olingo.jpa::RelationshipEntity" (
	"ID" BIGINT NOT NULL ,
    "SOURCE_ID" BIGINT,
	"Name" VARCHAR(255),
	"Type" VARCHAR(255),
	 PRIMARY KEY ("ID"))
;

insert into "org.apache.olingo.jpa::RelationshipEntity" values( 1, null, 'source 1', 'RelationshipSourceEntity');
insert into "org.apache.olingo.jpa::RelationshipEntity" values( 2, 1, 'rel 1_2', 'RelationshipTargetEntity');
insert into "org.apache.olingo.jpa::RelationshipEntity" values( 3, 1, 'rel 1_3', 'RelationshipTargetEntity');
insert into "org.apache.olingo.jpa::RelationshipEntity" values( 4, null, 'source 2', 'RelationshipSourceEntity');
insert into "org.apache.olingo.jpa::RelationshipEntity" values( 5, 4, 'rel 4_5', 'RelationshipTargetEntity');

--CREATE FUNCTION IS_PRIME(number Integer) RETURNS BOOLEAN
--       PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA 
--       EXTERNAL NAME 'org.apache.olingo.jpa.processor.core.test_udf.isPrime';
