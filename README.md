# LockServiceCentral

API that provides a simple interface for distributed locking with lease-based locks, supporting multiple backend systems. By leveraging a modular architecture, it ensures flexibility and scalability to meet diverse application needs.

## Overview

In distributed systems, managing concurrent access to shared resources is critical to maintaining data integrity and ensuring seamless operations. **LockServiceCentral** addresses this challenge by providing a REST-based API for acquiring, renewing, and releasing named locks for specific clients. The service employs JWT-based authentication to secure lock operations, though authentication can be disabled if required backend microservices are trusted and secured through other means.

**LockServiceCentral** defers the complex part of distributed locking to backend implementations. This modular approach allows a variety of backend systems to handle the intricate details of locking mechanisms.

## Features

- **Distributed Locking:** Manage access to shared resources across multiple instances and services.
- **Modular Backend Support:** Select the appropriate backend for your specific deployment.
- **JWT Protection:** Secure lock operations with JSON Web Tokens, ensuring locks are only accessible to authorized clients.
- **RESTful Interface:** Standardized API endpoints for lock management.
- **Lease-Based Locking:** Locks are held for a specified duration, automatically expiring if not renewed or released.
