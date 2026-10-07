# Camunda 8 Implementation Plan

## Purpose

This document converts the current repository review into a concrete plan for introducing Camunda 8 into this application. It covers:

- what exists today
- where Camunda 8 fits best
- current scalability and performance risks
- step-by-step implementation order
- necessary code and architecture changes for onboarding, scheduled tasks, and notifications

The goal is to add Camunda 8 as an orchestration layer without breaking the current domain model, MyBatis persistence, Liquibase migrations, and Spring Boot application structure.

## Current Repo Summary

This repository is a Spring Boot application with:

- MyBatis and MyBatis Plus
- Liquibase
- Spring Batch
- Spring Web and WebFlux
- Spring Security and OAuth2 resource server
- Kafka
- Keycloak admin client
- Thymeleaf templates for email
- scheduled jobs using `@Scheduled`

The current build file is [pom.xml](C:/Users/rvkri/IdeaProjects/ems/pom.xml).

At the time of review, the application does not contain any Camunda runtime integration:

- no Camunda 8 Zeebe client
- no Camunda job workers
- no BPMN resources
- no workflow engine configuration

This means the correct implementation approach is to introduce Camunda 8 gradually around business processes that are currently orchestrated in service code and schedulers.

## Core Conclusion

Camunda 8 should be used here for orchestration, timers, retries, user workflows, and cross-system coordination.

Camunda 8 should not replace:

- MyBatis persistence
- Liquibase schema management
- simple CRUD endpoints
- bulk SQL-heavy operations that are better handled by domain services

The best use of Camunda 8 in this application is:

1. quarterly review workflow
2. employee onboarding workflow
3. leave approval workflow
4. notification scheduling and retry handling

## Current Scalability and Performance Risks

Before or during Camunda adoption, these issues should be addressed because they will otherwise remain bottlenecks.

### 1. Unbounded raw thread creation

The code currently creates unmanaged threads directly:

- [CommunicationServiceImpl.java:79](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java#L79)
- [CommunicationServiceImpl.java:96](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java#L96)
- [CommunicationServiceImpl.java:114](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java#L114)
- [EmployeeServiceImpl.java:333](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeeServiceImpl.java#L333)
- [PasswordServiceImpl.java:132](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/PasswordServiceImpl.java#L132)

Problems:

- no thread pool limits
- no backpressure
- no retry visibility
- no monitoring
- failure handling is weak

Camunda 8 job workers should replace these fire-and-forget thread patterns for business-critical asynchronous steps.

### 2. Blocking external calls behind ad hoc async wrappers

Email sending uses `WebClient` but calls `.block()`:

- [CommunicationServiceImpl.java:148](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java#L148)

Problems:

- thread stays occupied until remote mail API finishes
- retry logic is not centralized
- failures are not persisted as incidents

Camunda 8 is a better place to run email delivery as retriable service tasks.

### 3. Row-by-row update loops

Quarter review transitions and period assignments update rows one by one:

- [ReviewTimelineServiceImpl.java:96](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/ReviewTimelineServiceImpl.java#L96)
- [ReviewTimelineServiceImpl.java:105](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/ReviewTimelineServiceImpl.java#L105)
- [EmployeePeriodServiceImpl.java:74](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeePeriodServiceImpl.java#L74)
- [EmployeePeriodServiceImpl.java:95](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeePeriodServiceImpl.java#L95)

Problems:

- too many DB round trips
- poor throughput
- weak bulk processing behavior

Camunda should orchestrate these transitions, but bulk DB updates should still be implemented with optimized DAO methods.

### 4. Nested async patterns

`@Async` is combined with `CompletableFuture.runAsync(...)`:

- [EmployeePeriodServiceImpl.java:183](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeePeriodServiceImpl.java#L183)

Problems:

- unpredictable concurrency model
- difficult operational control
- hard to tune thread usage

This should be replaced with either:

- normal synchronous service methods called by Camunda workers
- or a bounded executor if asynchronous execution is still needed outside Camunda

### 5. Leave flow is N+1 and loop heavy

Current leave processing has multiple scale issues:

- duplicate checks loop through all existing leaves for each requested leave
- inserts happen one by one
- manager approval reloads each leave and employee repeatedly

Relevant files:

- [LeaveServiceImpl.java:74](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/LeaveServiceImpl.java#L74)
- [LeaveServiceImpl.java:97](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/LeaveServiceImpl.java#L97)
- [LeaveServiceImpl.java:156](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/LeaveServiceImpl.java#L156)
- [LeaveDao.xml](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/mapper/LeaveDao.xml)

Camunda can improve approval orchestration, but SQL and DAO patterns still need optimization.

### 6. Small connection pool

The datasource pool is configured with only 5 connections:

- [application.yml:27](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/application.yml#L27)

This is likely too small for:

- REST traffic
- scheduled flows
- Spring Batch jobs
- notification delivery
- future Camunda workers

### 7. Production-hostile SQL logging

Current config enables verbose DB logging:

- [application.yml:46](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/application.yml#L46)
- [application.yml:91](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/application.yml#L91)
- [application.yml:123](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/application.yml#L123)

This should be restricted to local or debug profiles only.

### 8. Schema management conflict

JPA `ddl-auto: update` is enabled:

- [application.yml:50](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/application.yml#L50)

Liquibase is already present. Camunda 8 adoption should stay migration-driven and avoid runtime schema mutation behavior.

### 9. Scheduler-based orchestration is hard to operate

Business orchestration is hidden in `@Scheduled` methods:

- [ScheduledTasks.java:48](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java#L48)
- [ScheduledTasks.java:88](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java#L88)
- [ScheduledTasks.java:117](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java#L117)
- [ScheduledTasks.java:146](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java#L146)

Camunda 8 provides:

- durable timers
- incidents
- replayability
- operational visibility
- workflow-level audit trail

## Best Camunda 8 Use Cases In This Application

### 1. Quarterly Review Workflow

This is the strongest Camunda 8 candidate.

Current implementation is spread across:

- [ScheduledTasks.java:117](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java#L117)
- [ScheduledTasks.java:146](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java#L146)
- [ReviewTimelineServiceImpl.java:93](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/ReviewTimelineServiceImpl.java#L93)
- [ReviewTimelineDao.xml:51](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/mapper/ReviewTimelineDao.xml#L51)

Current behavior:

- quarter start is triggered by cron
- timelines are updated in loops
- notifications are sent in bulk loops
- state changes are embedded in service methods

Camunda 8 design:

- one process instance per employee review timeline or per employee-period-quarter
- BPMN timers for:
  - pre-start notification
  - start date
  - overdue date
  - lock date
  - completion/end date
- service tasks for:
  - mark review as started
  - send pre-start email
  - send start email
  - mark overdue
  - lock review
  - complete review

Expected benefit:

- no cron-driven bulk orchestration
- better retry handling
- clear visibility of which employee review is at which step

### 2. Employee Onboarding Workflow

Current onboarding is a multi-step distributed flow:

- create Keycloak user
- persist employee
- create password
- create department optionally
- create profile
- assign employee period
- generate review timelines
- send onboarding email
- send notifications

Relevant code:

- [EmployeeServiceImpl.java:123](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeeServiceImpl.java#L123)
- [EmployeeServiceImpl.java:133](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeeServiceImpl.java#L133)
- [EmployeeServiceImpl.java:154](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeeServiceImpl.java#L154)
- [EmployeeServiceImpl.java:156](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeeServiceImpl.java#L156)

Camunda 8 design:

- one process instance per onboarding request
- BPMN service tasks:
  - validate onboarding request
  - create Keycloak user
  - save employee
  - save password
  - save department if missing
  - save profile
  - assign employee period
  - generate review timelines
  - send onboarding email
  - notify manager
- compensation strategy for partial failures

Expected benefit:

- onboarding no longer depends on one synchronous request path
- partial failure recovery becomes explicit
- side effects can be retried independently

### 3. Leave Approval Workflow

Current leave flow is a good user task candidate.

Relevant code:

- [LeaveServiceImpl.java:74](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/LeaveServiceImpl.java#L74)
- [LeaveServiceImpl.java:150](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/LeaveServiceImpl.java#L150)

Camunda 8 design:

- process instance per leave request
- manager approval as a user task
- timer events for reminders and escalation
- service tasks for:
  - create leave entry
  - validate manager access
  - notify manager
  - update final status
  - notify employee

Expected benefit:

- approval visibility
- timeout handling
- escalation support
- simpler future policy changes

## Camunda 8 Adoption Principles For This Repo

### Keep Camunda for orchestration only

Camunda should manage:

- workflow state
- timers
- retries
- incidents
- correlation

The existing application should continue to own:

- domain tables
- MyBatis SQL
- Liquibase migrations
- validation logic
- DTOs and controllers

### Do not move core persistence into workflow workers

Workers should call existing service methods. This reduces migration risk and avoids duplicating business rules.

### Use process business keys consistently

Examples:

- onboarding: `employeeEmail` or request UUID
- quarterly review: `employeePeriodUuid + reviewType`
- leave approval: `leaveUuid`

### Add workflow linkage to domain entities only where useful

Optional columns can be added later:

- `workflow_instance_key`
- `workflow_definition_id`
- `workflow_status`

These are useful for traceability but should not be required in phase 1.

## Recommended Implementation Order

### Phase 0. Preparation

Prepare the application so Camunda 8 can be added cleanly.

Steps:

1. remove unmanaged `new Thread(...)` usage gradually
2. isolate notification sending behind a clear service interface
3. isolate Keycloak operations behind a retry-safe adapter
4. reduce blocking and loop-heavy orchestration in services
5. move noisy logging to local profile only
6. ensure Liquibase is the only schema change mechanism

### Phase 1. Add Camunda 8 Technical Foundation

Add the Camunda 8 base integration without changing business behavior yet.

Steps:

1. add Camunda 8 dependencies to [pom.xml](C:/Users/rvkri/IdeaProjects/ems/pom.xml)
2. add Camunda connection properties to [application.yml](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/application.yml)
3. create a dedicated config package for Camunda
4. add a small proof-of-life workflow
5. create first job worker beans
6. verify worker registration and connectivity

Suggested structure:

```text
src/main/java/com/learning/emsmybatisliquibase/camunda/
  config/
  worker/
  workflow/
  mapper/
src/main/resources/bpmn/
```

### Phase 2. Migrate Notifications First

This is the safest first business use case because it is asynchronous and side-effect oriented.

Why start here:

- low domain risk
- high operational value
- current implementation already behaves asynchronously, but unsafely

Steps:

1. define a BPMN process for sending notifications
2. create workers for:
   - render template
   - send email
   - update notification status
3. replace `new Thread(...)` email sending with workflow start commands
4. persist correlation information if needed
5. add retry strategy and incident review

Candidate replacements:

- [CommunicationServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java)

### Phase 3. Migrate Scheduled Tasks

After notifications, replace scheduler-driven business orchestration with workflow timers.

Why second:

- scheduled review flows already depend on notifications
- timers are a natural Camunda strength

Target code:

- [ScheduledTasks.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java)
- [ReviewTimelineServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/ReviewTimelineServiceImpl.java)

Migration steps:

1. keep the scheduler initially, but change it to start workflows instead of doing business work directly
2. move quarter lifecycle logic into BPMN timers and workers
3. move before-start notification into timer-based process events
4. move start and completion transitions into workers
5. remove bulk cron logic after workflow timers are stable

Interim safe approach:

- scheduler triggers process creation
- workflow manages the rest

Final approach:

- workflow timers fully replace the scheduler for review lifecycle orchestration

### Phase 4. Migrate Employee Onboarding

After the async infrastructure is stable, move onboarding into a workflow.

Target code:

- [EmployeeServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeeServiceImpl.java)
- [PasswordServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/PasswordServiceImpl.java)
- [EmployeePeriodServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeePeriodServiceImpl.java)

Migration steps:

1. split onboarding into clearly callable service methods
2. keep controller contract unchanged initially
3. controller starts workflow instead of executing all side effects inline
4. worker sequence:
   - create Keycloak account
   - save employee
   - save password
   - save department
   - save profile
   - assign employee period
   - generate timelines
   - send email
   - send app notifications
5. add compensation or failure handling

Initial controller behavior options:

- synchronous submit and return tracking ID
- synchronous submit and poll status

Recommended first option:

- return accepted response with workflow correlation ID

### Phase 5. Migrate Leave Approval

After onboarding and notifications are stable, move leave approval to BPMN user-task flow.

Migration steps:

1. create a leave request process definition
2. start process when leave is created
3. create manager approval task
4. add reminder timer
5. add escalation timer if needed
6. update leave state through workers
7. notify employee on approval or rejection

## Detailed Step-By-Step Startup Plan

This section is the practical implementation sequence.

### Step 1. Add Camunda 8 dependencies

Update [pom.xml](C:/Users/rvkri/IdeaProjects/ems/pom.xml) with Camunda 8 Spring Boot support and client libraries appropriate for your chosen deployment model.

Target outcome:

- application can register workers
- application can deploy BPMN
- application can start process instances

### Step 2. Add configuration properties

Extend [application.yml](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/application.yml) with a dedicated section for Camunda 8.

Add environment-driven properties for:

- Camunda cluster or local endpoint
- authentication credentials if using SaaS
- worker tuning
- BPMN deployment toggle

Example property groups to introduce:

```yaml
camunda:
  client:
    mode: self-managed
    zeebe:
      grpc-address: ${CAMUNDA_ZEEBE_GRPC_ADDRESS}
      rest-address: ${CAMUNDA_ZEEBE_REST_ADDRESS}
    auth:
      client-id: ${CAMUNDA_CLIENT_ID:}
      client-secret: ${CAMUNDA_CLIENT_SECRET:}
      audience: ${CAMUNDA_AUDIENCE:}
  worker:
    max-jobs-active: 32
    threads: 8
```

The exact final format depends on the Camunda 8 Spring integration version you choose.

### Step 3. Create BPMN resource folder

Add:

```text
src/main/resources/bpmn/
```

Initial BPMN files:

- `notification-send.bpmn`
- `quarterly-review-lifecycle.bpmn`
- `employee-onboarding.bpmn`
- later: `leave-approval.bpmn`

### Step 4. Create Camunda configuration package

Add Java packages:

```text
com.learning.emsmybatisliquibase.camunda.config
com.learning.emsmybatisliquibase.camunda.worker
com.learning.emsmybatisliquibase.camunda.workflow
```

Responsibilities:

- config: client and worker setup
- worker: job worker implementations
- workflow: process starter services and workflow variable models

### Step 5. Introduce a process starter service

Do not start workflows directly from controllers at first.

Create a service layer such as:

- `CamundaWorkflowService`
- `OnboardingWorkflowStarter`
- `ReviewWorkflowStarter`
- `NotificationWorkflowStarter`

This keeps controllers and existing services clean.

### Step 6. Implement notification workflow first

Workflow:

1. start process
2. render template
3. call email provider
4. mark success
5. retry on failure

Necessary changes:

- refactor [CommunicationServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java)
- replace direct thread spawning with workflow start
- create email worker
- keep `sendEmail(...)` logic in a reusable adapter class

### Step 7. Implement review lifecycle workflow

Workflow:

1. wait until review pre-start date
2. send pre-start reminder
3. wait until start date
4. mark review as started
5. send start email
6. wait until overdue date
7. mark overdue if incomplete
8. wait until lock date
9. lock if still incomplete
10. wait until end date
11. complete and close

Necessary changes:

- reduce direct use of [ScheduledTasks.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java)
- move date-based transition logic into workers
- keep domain updates in existing services

### Step 8. Implement onboarding workflow

Workflow:

1. receive onboarding request
2. validate
3. create Keycloak user
4. save employee
5. create password
6. create profile
7. create or assign department
8. assign employee period
9. generate review timelines
10. send onboarding email
11. send app notifications

Necessary changes:

- split [EmployeeServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeeServiceImpl.java) into smaller internal actions if needed
- make each side effect idempotent where possible
- return workflow tracking identifier from onboarding initiation

### Step 9. Implement leave approval workflow

Workflow:

1. create leave request
2. validate duplicate or invalid leave
3. notify manager
4. create approval task
5. wait for manager action
6. approve or reject
7. notify employee

Necessary changes:

- keep leave DB writes inside leave service
- use workflow for manager action and timeout handling

### Step 10. Remove old scheduler and thread orchestration

Once workflows are stable:

- remove business logic from `@Scheduled` methods
- keep only startup or migration helpers if needed
- remove direct `new Thread(...)` usage
- reduce use of nested `@Async` and `CompletableFuture.runAsync(...)`

## Necessary Code Changes By Area

### A. `pom.xml`

Add:

- Camunda 8 client dependencies
- Spring integration for Camunda 8

Review and keep compatible with:

- Spring Boot version
- existing WebFlux and Web dependencies
- existing Keycloak and security dependencies

### B. `application.yml`

Change or add:

- Camunda 8 connection settings
- worker tuning settings
- environment-specific logging
- disable production SQL trace
- revisit Hikari pool sizing
- plan removal of `ddl-auto: update`

### C. Notification services

Refactor:

- [CommunicationServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java)

Change:

- move direct thread creation out
- move direct external delivery into worker-safe adapter
- support idempotent retries

### D. Scheduled task layer

Refactor:

- [ScheduledTasks.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java)

Change:

- scheduler should stop owning review lifecycle business rules
- scheduler may temporarily become workflow bootstrapper only

### E. Review timeline services

Refactor:

- [ReviewTimelineServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/ReviewTimelineServiceImpl.java)
- [ReviewTimelineDao.xml](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/mapper/ReviewTimelineDao.xml)

Change:

- create bulk update SQL methods where helpful
- separate orchestration from state mutation

### F. Employee onboarding services

Refactor:

- [EmployeeServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeeServiceImpl.java)
- [EmployeePeriodServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeePeriodServiceImpl.java)
- [PasswordServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/PasswordServiceImpl.java)

Change:

- extract reusable, idempotent domain actions
- stop chaining multiple side effects inline in one request
- route the orchestration path through workflow

### G. Leave service

Refactor:

- [LeaveServiceImpl.java](C:/Users/rvkri/IdeaProjects/ems/src/main/java/com/learning/emsmybatisliquibase/service/impl/LeaveServiceImpl.java)
- [LeaveDao.xml](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/mapper/LeaveDao.xml)

Change:

- improve duplicate detection with DB constraints or optimized queries
- add bulk update methods for approvals if needed
- keep workflow focused on approval lifecycle

## Suggested First BPMN Processes

### 1. Notification Send Process

Input:

- template name
- recipient email
- recipient name
- payload variables
- correlation ID

Tasks:

- build content
- send email
- handle failure

### 2. Quarterly Review Lifecycle Process

Input:

- employee UUID
- employee period UUID
- review timeline UUID
- review type
- start, overdue, lock, and end timestamps

Tasks:

- pre-start reminder
- start review
- start notification
- mark overdue
- lock review
- complete review

## Quarterly Review Workflow: Detailed Implementation Plan

### Recommended process-instance boundary

Use **one Camunda process instance per `review_timeline` row**.

This is preferable to one process per quarter because every timeline already has its own:

- employee-period association
- review type
- start time
- overdue time
- lock time
- end time
- lifecycle status

Use `reviewTimelineUuid` as the process business key and primary correlation identifier. Store only identifiers and timer timestamps as process variables; reload mutable employee and review data from PostgreSQL inside workers.

Do not use one process instance for all employees in a quarter. A bulk process would recreate the current loop-heavy behavior, make individual failures harder to retry, and reduce operational visibility.

### Target BPMN flow

Create:

```text
src/main/resources/bpmn/quarterly-review-lifecycle.bpmn
```

Model the process as:

```text
Start
  -> Wait until preStartTime
  -> Send pre-start email
  -> Wait until startTime
  -> Mark timeline STARTED
  -> Send start email
  -> Wait until overdueTime
  -> Mark timeline OVERDUE when still STARTED
  -> Wait until lockTime
  -> Mark timeline LOCKED when still STARTED or OVERDUE
  -> Wait until endTime
  -> Mark timeline COMPLETED when not already COMPLETED
  -> End
```

Use intermediate timer catch events with date expressions:

```text
=preStartTime
=startTime
=overdueTime
=lockTime
=endTime
```

Use these service-task job types:

```text
review.send-pre-start-email
review.mark-started
review.send-start-email
review.mark-overdue
review.lock
review.complete
```

The timer values passed to Camunda must include an offset or UTC timezone. The current entity uses `LocalDateTime`, so the workflow starter must convert each value using the configured business timezone, currently `Asia/Kolkata`, to an ISO-8601 offset timestamp before starting the process.

### Process variables

Create:

```text
src/main/java/com/learning/emsmybatisliquibase/camunda/workflow/review/QuarterlyReviewVariables.java
```

Variables:

```text
reviewTimelineUuid
employeePeriodUuid
reviewType
preStartTime
startTime
overdueTime
lockTime
endTime
```

Do not put employee name, email address, review content, or the entire `ReviewTimeline` object into workflow variables. Workers must query current domain data by `reviewTimelineUuid`.

Derive `preStartTime` from a configurable duration, initially seven days before `startTime`, because the current scheduler sends the notification on March/June/September/December 25 before the next quarter starts. Add:

```yaml
ems:
  review-workflow:
    pre-start-notification-days: 7
    business-zone: Asia/Kolkata
    bootstrap-page-size: 500
```

### Step 1. Add Camunda technical foundation

Change:

- `pom.xml`
- `src/main/resources/application.yml`
- environment-specific deployment configuration

Add the Camunda 8 Spring client dependency compatible with the repository's Spring Boot version. This repository currently uses Spring Boot `4.0.5`; verify Camunda's supported Spring Boot compatibility before choosing the client version. Do not force an incompatible Camunda starter into the application.

Configure:

- Zeebe gRPC/REST endpoint
- authentication for SaaS or self-managed deployment
- BPMN deployment
- worker thread count
- maximum active jobs
- job timeout
- retry/backoff defaults

Add a Camunda health check and verify that the application can deploy a BPMN model and register a test worker before migrating review behavior.

### Step 2. Add workflow linkage and idempotency columns

Create a new Liquibase migration:

```text
src/main/resources/db/changelog/migration/review-timeline-workflow-ddl.xml
```

Add nullable columns to `review_timeline`:

```text
workflow_instance_key BIGINT
workflow_status VARCHAR
workflow_started_time TIMESTAMP
```

Add a unique index on `workflow_instance_key` when it is not null. Also add or verify indexes supporting:

```text
review_timeline(status, start_time)
review_timeline(employee_period_uuid, type)
```

Map the new columns in:

- `src/main/java/com/learning/emsmybatisliquibase/entity/ReviewTimeline.java`
- `src/main/resources/mapper/ReviewTimelineDao.xml`

These columns let the bootstrapper avoid starting duplicate instances and let support staff navigate from the domain record to Camunda Operate.

### Step 3. Split lifecycle mutations from orchestration

Change:

- `src/main/java/com/learning/emsmybatisliquibase/service/ReviewTimelineService.java`
- `src/main/java/com/learning/emsmybatisliquibase/service/impl/ReviewTimelineServiceImpl.java`
- `src/main/java/com/learning/emsmybatisliquibase/dao/ReviewTimelineDao.java`
- `src/main/resources/mapper/ReviewTimelineDao.xml`

Add worker-safe, idempotent domain methods:

```java
boolean markStarted(UUID timelineUuid);
boolean markOverdue(UUID timelineUuid);
boolean lock(UUID timelineUuid);
boolean complete(UUID timelineUuid);
```

Implement each transition as a conditional SQL update instead of loading a collection and updating rows in a loop. Examples:

```sql
UPDATE review_timeline
SET status = 'STARTED', updated_time = CURRENT_TIMESTAMP
WHERE uuid = #{uuid}
  AND status IN ('SCHEDULED', 'NOT_STARTED');
```

```sql
UPDATE review_timeline
SET status = 'OVERDUE', updated_time = CURRENT_TIMESTAMP
WHERE uuid = #{uuid}
  AND status = 'STARTED';
```

```sql
UPDATE review_timeline
SET status = 'LOCKED', updated_time = CURRENT_TIMESTAMP
WHERE uuid = #{uuid}
  AND status IN ('STARTED', 'OVERDUE');
```

```sql
UPDATE review_timeline
SET status = 'COMPLETED', updated_time = CURRENT_TIMESTAMP
WHERE uuid = #{uuid}
  AND status != 'COMPLETED';
```

A zero-row update must be treated as an idempotent no-op when the timeline is already at or beyond the requested state. It should fail only when the timeline does not exist or the current state represents an invalid transition.

Correct the existing DAO signature while making these changes:

```java
findByStatusAndReviewType(ReviewTimelineStatus status, ReviewType reviewType)
```

It currently accepts `PeriodStatus` while filtering `review_timeline.status`.

After workers are live, deprecate and then remove `startTimelinesForQuarter(...)`; it mixes completion, start, loops, and notification orchestration in one service method.

### Step 4. Add a workflow starter and bootstrap query

Create:

```text
src/main/java/com/learning/emsmybatisliquibase/camunda/workflow/review/QuarterlyReviewWorkflowStarter.java
src/main/java/com/learning/emsmybatisliquibase/camunda/workflow/review/QuarterlyReviewWorkflowBootstrapper.java
```

Add DAO methods that page through eligible timelines:

```java
List<ReviewTimeline> findEligibleForWorkflowStart(int limit, UUID afterUuid);
int attachWorkflowInstance(UUID timelineUuid, long processInstanceKey);
```

Eligibility should require:

- timeline has all required dates
- timeline status is `SCHEDULED` or `NOT_STARTED`
- active employee period
- `workflow_instance_key IS NULL`

For each eligible row:

1. validate `preStartTime < startTime < overdueTime < lockTime <= endTime`
2. start `quarterly-review-lifecycle` using `reviewTimelineUuid` as business key
3. persist the returned process instance key
4. record bootstrap failures for retry and monitoring

The starter must be callable immediately after a new timeline is inserted. Also keep a temporary paged reconciliation scheduler so timelines created before the migration or missed after transient failures are eventually started.

### Step 5. Start workflows when timelines are created

Change:

- `src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeePeriodServiceImpl.java`

The current timeline creation path inserts each timeline through `reviewTimelineDao.insert(...)`. After the surrounding database transaction commits, invoke `QuarterlyReviewWorkflowStarter` for each newly created timeline.

Do not start the Camunda process before the database transaction commits; otherwise a worker can run before the timeline row is visible. Use an after-commit event/listener or an outbox record. For the first rollout, the reconciliation bootstrapper remains the recovery mechanism if an after-commit start fails.

### Step 6. Implement review lifecycle workers

Create:

```text
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/MarkReviewStartedWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/MarkReviewOverdueWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/LockReviewWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/CompleteReviewWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/SendReviewPreStartEmailWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/SendReviewStartEmailWorker.java
```

Worker rules:

- accept `reviewTimelineUuid` as the required input
- call one domain service method
- contain no direct SQL
- complete only after the domain operation succeeds
- throw retryable failures for transient DB/email-provider errors
- create an incident after configured retries are exhausted
- treat an already-applied state transition as success

The state workers should be small wrappers around `ReviewTimelineService`. The email workers should call a single-recipient communication method rather than loading and sending an entire quarter in one job.

### Step 7. Refactor notifications for one timeline at a time

Change:

- `src/main/java/com/learning/emsmybatisliquibase/service/CommunicationService.java`
- `src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java`
- `src/main/java/com/learning/emsmybatisliquibase/dao/ReviewTimelineDao.java`
- `src/main/resources/mapper/ReviewTimelineDao.xml`

Add:

```java
void sendReviewPreStartNotification(UUID timelineUuid);
void sendReviewStartNotification(UUID timelineUuid);
```

Add a DAO query that returns one `NotificationDto` by timeline UUID. Remove the raw `new Thread(...)` behavior from the new methods; Camunda already provides asynchronous execution and retries.

For duplicate-send protection, persist a delivery key such as:

```text
review:{timelineUuid}:pre-start
review:{timelineUuid}:start
```

Reuse or extend the notification table so the worker can detect an already successful delivery before calling the email provider again. Without this, a worker timeout after a successful provider call can send a duplicate email on retry.

Keep the existing bulk notification methods only during the parallel-run period, then remove:

```java
sendNotificationBeforeStart(List<NotificationDto>, ReviewType)
sendStartNotification(ReviewType)
```

### Step 8. Replace the review schedulers safely

Change:

- `src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java`

Roll out in two stages:

1. Disable business actions in `startTimeline()` and `sendBeforeStartNotification()`, but keep a reconciliation scheduler that starts missing workflow instances.
2. After at least one complete quarter transition and reconciliation verification, remove both old review scheduler methods.

Do not run the old schedulers and Camunda lifecycle workers as active writers at the same time. Although conditional updates protect status changes, both paths can still send duplicate emails.

Keep unrelated scheduled methods in `ScheduledTasks`, including period, profile, and OTP tasks, until they are migrated separately.

### Step 9. Define handling for changed timeline dates

BPMN timer due dates are created from the variables present when the process reaches each timer. Updating `review_timeline.start_time`, `overdue_time`, `lock_time`, or `end_time` after process start does not automatically update a timer that is already active.

Choose and implement one policy before production rollout:

1. Recommended initial policy: reject lifecycle date edits after `workflow_instance_key` is assigned.
2. Later policy: publish a timeline-rescheduled command and use a supported Camunda process-instance modification/migration procedure to recreate affected timers.

Document the selected policy in the review timeline update API and validate it in `ReviewTimelineServiceImpl`.

### Step 10. Test the workflow

Add:

```text
src/test/java/com/learning/emsmybatisliquibase/camunda/workflow/review/QuarterlyReviewWorkflowTest.java
src/test/java/com/learning/emsmybatisliquibase/camunda/worker/review/ReviewLifecycleWorkerTest.java
src/test/java/com/learning/emsmybatisliquibase/service/impl/ReviewTimelineServiceImplTest.java
```

Required test cases:

- starts one process per eligible timeline
- does not start a second process for an attached timeline
- converts `Asia/Kolkata` `LocalDateTime` values to the correct offset timestamps
- sends pre-start and start notification once
- transitions `SCHEDULED -> STARTED -> OVERDUE -> LOCKED -> COMPLETED`
- skips overdue/lock transitions when the review is already completed
- worker retry does not duplicate a state transition or notification
- invalid or missing dates prevent workflow start and are observable
- inactive employee-period timelines are not bootstrapped
- bootstrap pagination processes all eligible rows

Use short timer durations in an integration-test BPMN variant or inject dates close to the test clock. Do not wait for real quarter dates in tests.

### Step 11. Production rollout and acceptance criteria

Rollout sequence:

1. deploy schema and worker code with review workers disabled
2. deploy and validate the BPMN definition
3. start workflows for a small controlled set of timelines
4. verify timers and variables in Camunda Operate
5. enable workers and verify status transitions and single-recipient emails
6. run the reconciliation bootstrapper for all open timelines
7. disable the two legacy review scheduler methods
8. monitor through one complete quarter transition
9. remove deprecated bulk orchestration methods

Acceptance criteria:

- every open timeline has exactly one workflow instance key
- no review lifecycle transition depends on the quarterly cron methods
- no review email is sent through a bulk loop or raw thread
- worker retries are idempotent
- incidents identify the affected `reviewTimelineUuid`
- support can trace a timeline from PostgreSQL to its Camunda instance
- reconciliation reports zero eligible timelines without a workflow instance

### File-change summary for the quarterly review migration

Modify:

```text
pom.xml
src/main/resources/application.yml
src/main/java/com/learning/emsmybatisliquibase/entity/ReviewTimeline.java
src/main/java/com/learning/emsmybatisliquibase/dao/ReviewTimelineDao.java
src/main/resources/mapper/ReviewTimelineDao.xml
src/main/java/com/learning/emsmybatisliquibase/service/ReviewTimelineService.java
src/main/java/com/learning/emsmybatisliquibase/service/impl/ReviewTimelineServiceImpl.java
src/main/java/com/learning/emsmybatisliquibase/service/CommunicationService.java
src/main/java/com/learning/emsmybatisliquibase/service/impl/CommunicationServiceImpl.java
src/main/java/com/learning/emsmybatisliquibase/service/impl/EmployeePeriodServiceImpl.java
src/main/java/com/learning/emsmybatisliquibase/scheduled/ScheduledTasks.java
```

Add:

```text
src/main/resources/bpmn/quarterly-review-lifecycle.bpmn
src/main/resources/db/changelog/migration/review-timeline-workflow-ddl.xml
src/main/java/com/learning/emsmybatisliquibase/camunda/workflow/review/QuarterlyReviewVariables.java
src/main/java/com/learning/emsmybatisliquibase/camunda/workflow/review/QuarterlyReviewWorkflowStarter.java
src/main/java/com/learning/emsmybatisliquibase/camunda/workflow/review/QuarterlyReviewWorkflowBootstrapper.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/MarkReviewStartedWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/MarkReviewOverdueWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/LockReviewWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/CompleteReviewWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/SendReviewPreStartEmailWorker.java
src/main/java/com/learning/emsmybatisliquibase/camunda/worker/review/SendReviewStartEmailWorker.java
src/test/java/com/learning/emsmybatisliquibase/camunda/workflow/review/QuarterlyReviewWorkflowTest.java
src/test/java/com/learning/emsmybatisliquibase/camunda/worker/review/ReviewLifecycleWorkerTest.java
```

Remove after successful rollout:

```text
ScheduledTasks.startTimeline()
ScheduledTasks.sendBeforeStartNotification()
ReviewTimelineService.startTimelinesForQuarter(...)
CommunicationService.sendNotificationBeforeStart(...)
CommunicationService.sendStartNotification(...)
ReviewTimelineDao.getTimelineIdsByReviewType(...)
```

### 3. Employee Onboarding Process

Input:

- onboarding request fields

Tasks:

- validate
- create identity
- persist employee
- persist password
- persist profile
- assign period
- create timelines
- notify employee
- notify manager

## Operational Recommendations

### Worker tuning

Start conservatively:

- bounded worker thread pool
- moderate `maxJobsActive`
- clear retry counts and backoff

### Idempotency

Required for workers that call:

- Keycloak
- email API
- DB mutations

Examples:

- do not create the same employee twice
- do not send the same onboarding email twice without detection
- do not assign the same timeline twice

### Monitoring

Track:

- workflow start rate
- worker failure rate
- average job duration
- incidents by process type
- external API error rates

### Database

Before scaling workers, review:

- connection pool size
- index coverage for review and leave queries
- bulk update support in DAOs

## What Not To Do

Do not:

- move all CRUD into Camunda immediately
- replace MyBatis with workflow variables
- keep using raw threads after worker adoption
- mix business timers between scheduler and BPMN permanently
- start with onboarding before notifications and worker infrastructure are stable

## Recommended Immediate Next Actions

1. update [pom.xml](C:/Users/rvkri/IdeaProjects/ems/pom.xml) with Camunda 8 base dependencies
2. add Camunda 8 config section to [application.yml](C:/Users/rvkri/IdeaProjects/ems/src/main/resources/application.yml)
3. create `src/main/resources/bpmn/notification-send.bpmn`
4. refactor notification sending to start a workflow instead of spawning raw threads
5. implement first email worker
6. then migrate quarterly review timers
7. then migrate onboarding

## Final Recommendation

The best rollout path for this application is:

1. foundation and worker setup
2. notifications
3. scheduled review lifecycle
4. onboarding
5. leave approval

This order keeps risk low, produces value early, and aligns Camunda 8 with the places where this codebase currently has the most orchestration complexity and the weakest scalability behavior.
