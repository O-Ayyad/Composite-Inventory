# Composite Inventory
<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-blue.svg" />
  <img src="https://img.shields.io/badge/Build-Maven-red.svg" />
  <img src="https://img.shields.io/badge/UI-Swing-green.svg" />
  <img src="https://img.shields.io/badge/License-Open%20Source-lightgrey.svg" />
</p>

Composite Inventory is a Java desktop application designed to help small businesses manage inventory for composite products (that is, items made from multiple components). It automates quantity tracking, SKU mapping across marketplaces, and inventory adjustments when products are assembled, disassembled, or reserved for orders.
Composite Inventory is intended for business with complicated 

---

##  Features

-  Track both simple and composite inventory items  
-  Compose and break down products into component parts  
-  Reserve inventory for marketplace orders  
-  SKU mapping across multiple platforms 
-  Automatic background saving  
-  Auto-fetch platform updates  
-  Inventory logging and change tracking  
-  Desktop GUI built with Java Swing  

## Tech Stack


- **Language:** Java  
- **UI Framework:** Swing  
- **Build Tool:** Maven  
- **Serialization:** Gson  
- **Image Support:** TwelveMonkeys ImageIO  
- **Packaging:** Maven Shade Plugin

---

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/O-Ayyad/Composite-Inventory.git
cd Composite-Inventory
```

### 2. Build the Project

```bash
mvn clean package
```

### 3. Run the Application

```bash
java -jar target/Composite-Inventory-1.0.jar
```

---

## Use Cases


Composite Inventory is ideal for:


- E-commerce sellers bundling products
- Businesses selling kits or assemblies
- Multi-platform sellers needing centralized inventory control
- Operations that require accurate component-level stock tracking

---

##  License

This project is open-source and available for educational and personal use.
