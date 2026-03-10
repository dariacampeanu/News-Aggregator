# Java News Aggregator

## Project Overview
This project is a high-performance news processing engine built in Java. It is designed to handle large-scale datasets by leveraging multithreading to parse, filter, and analyze thousands of JSON files in parallel. The system ensures data integrity through advanced synchronization while maintaining high throughput.

## Core Features
- Multithreaded Processing: Implements a 5-phase execution model to handle data extraction and analysis.
- Smart Deduplication: Identifies and filters duplicate articles based on unique UUIDs and titles.
- Automated Analytics: Generates detailed reports on author activity, category distribution, and language-specific keyword frequency.
- Thread-Safe Architecture: Utilizes ConcurrentHashMap, ConcurrentLinkedQueue, and AtomicInteger to manage shared data without race conditions.
- Synchronization: Uses CyclicBarrier to coordinate thread phases, ensuring each step of the pipeline is completed before moving forward.

## Technical Strategy
The application uses a round-robin distribution strategy, where each thread is assigned specific indices to process. This approach prevents complex dynamic allocation overhead and ensures a balanced workload across all available CPU cores.

## Performance and Scalability
Testing conducted on an Apple M4 system (10 cores, 16GB RAM) demonstrated significant performance gains:
- The system achieved a speedup of 2.64x when moving from 1 to 3 threads.
- Efficiency remained high at 88% for 3 threads, showing that the synchronization overhead is well-managed for large datasets.
- The analysis shows that for the tested dataset, 3 threads represent the optimal balance between performance and resource usage.

## Getting Started

### Prerequisites
- Java 21 LTS or higher
- Jackson Databind library (included in libs/)

### Installation and Build
The project includes a Makefile for easy management. To compile the source files, run:
```bash
make build
