
## Сущности

### User
- id (uuid)
- role (enum client, admin)
- email
- name
- phone_number
- createdAt
- updatedAt

### ServerRequest
- id
- ownerId
- cpuCores
- ramGb
- diskGb
- os
- status (enum created, approved, rejected, completed, cancelled)
- createdAt
- updatedAt

### Server
- id
- requestIdsHistory
- hostname
- ipv4Adresses
- ipv6Adresses
- status (enum off,on,suspended)
- createdAt
- updatedAt
