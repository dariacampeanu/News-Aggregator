# Java News Aggregator

## Project Overview
This project is a high-performance news aggregation system developed in Java. It is designed to process large volumes of news data by utilizing parallel execution to parse JSON files, filter content, and generate analytical reports. The system focuses on data integrity and processing speed through efficient thread management.

## Core Features
- Parallel Data Extraction: Simultaneously reads and parses multiple JSON data sources to minimize I/O wait times.
- Automated Deduplication: Identifies and removes redundant articles by cross-referencing unique UUIDs and titles.
- Content Analytics: Extracts and categorizes information regarding authors, languages, and topics.
- Keyword Analysis: Processes English-language articles to identify and frequency-rank key terms.
- Thread-Safe Reporting: Generates consolidated output files and statistical reports from the processed data.

## Technical Stack & Efficiency
The application is built with a focus on thread safety and low-latency synchronization:
- Multithreading: Uses a fixed-size thread pool with a round-robin workload distribution to ensure balanced CPU utilization.
- Synchronization: Implements CyclicBarrier to coordinate processing phases, ensuring consistent state transitions.
- Concurrent Collections: Utilizes ConcurrentHashMap and ConcurrentLinkedQueue for lock-free or low-lock data sharing.
- Atomic Operations: Employs AtomicInteger for accurate, high-speed counter management across multiple threads.
- JSON Processing: Integrated with the Jackson Databind library for efficient object mapping.

## Project Structure
- Tema1.java: Main entry point and execution flow control.
- WorkerThread.java: Implementation of the parallel processing logic and phases.
- SharedData.java: Centralized thread-safe storage for synchronized data access.
- Article.java: Data model representing a news entry.
- Utils.java: Utility class for file handling and path management.

