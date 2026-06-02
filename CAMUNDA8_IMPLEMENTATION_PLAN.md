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
