# Geetanjali Tailor Shop System — Master Reference (Part 4: Comprehensive Flowcharts & Scenarios)

> **Document Scope**: Part 4 of 4 — Complete visual topologies, sequence diagrams, and decision flowcharts covering every single file, execution path, branch condition, and failure mode.  
> **Project**: Geetanjali Tailor Shop Management Web Application  
> **Target Audience**: Student pair programmer learning Spring Boot during a 24-hour hackathon.  
> **Version / Git Commit**: `Stage 1 - Step 1: Shared foundation` (`1d14e6f`)

---

## Master File Coverage Matrix

Every file in the codebase is represented across the scenarios below:

| File Name | Primary Scenario(s) Covered | Role in Flow |
| :--- | :--- | :--- |
| [`BackendApplication.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/BackendApplication.java) | Scenario 1 | JVM entry point; boots Tomcat, Flyway, and Spring ApplicationContext |
| [`AppConfig.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/config/AppConfig.java) | Scenario 1, 4 | Produces the IST `Clock` bean for time-aware services |
| [`WebMvcConfig.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/config/WebMvcConfig.java) | Scenario 1, 6 | Registers `/uploads/**` static resource handler for cloth photos |
| [`SecurityConfig.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/security/SecurityConfig.java) | Scenario 1, 2, 3, 4, 5, 7, 8 | Evaluates HTTP request paths; enforces authentication; configures form login/logout |
| [`AppUserDetails.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/security/AppUserDetails.java) | Scenario 3, 4, 5 | Wraps `AppUser` entity into Spring Security principal session object |
| [`AppUserDetailsService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/security/AppUserDetailsService.java) | Scenario 3 | Queries `app_user` table by mobile number during form login |
| [`GlobalModelAdvice.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/advice/GlobalModelAdvice.java) | Scenario 4, 7A | Injects `${shopName}`, `${currentUser}`, `${currentUserName}`, `${isOwner}`, `${currentUri}` |
| [`GlobalExceptionHandler.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/advice/GlobalExceptionHandler.java) | Scenario 7A, 7B | Intercepts `ResourceNotFoundException` and `BusinessRuleException` |
| [`AuthController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/AuthController.java) | Scenario 2, 3, 5 | Handles `/login` and `/signup` GET and POST requests |
| [`HomeController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/HomeController.java) | Scenario 4 | Handles `GET /` and renders `home/index.html` |
| [`AuthService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/AuthService.java) | Scenario 2, 3, 5 | Evaluates `isFirstRun()`, `isMobileRegistered()`, registers owners and tailors |
| [`ShopService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/ShopService.java) | Scenario 4 | Queries `ShopRepository` to provide primary shop name |
| [`PhotoStorageService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/storage/PhotoStorageService.java) | Scenario 6 | Storage interface defining `store()` and `delete()` contracts |
| [`LocalDiskPhotoStorageService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/storage/LocalDiskPhotoStorageService.java) | Scenario 6 | Writes photos to disk under UUID names in `uploads/` |
| [`PhoneNumbers.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/util/PhoneNumbers.java) | Scenario 2, 5 | Normalizes Indian mobile numbers and validates 10-digit regex |
| [`SignupForm.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/dto/form/SignupForm.java) | Scenario 2, 5 | Form DTO holding signup fields with Jakarta validation |
| [`Role.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/enums/Role.java) | Scenario 2, 3, 4, 5 | Enum defining `OWNER` and `TAILOR` authorization |
| [`ResourceNotFoundException.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/exception/ResourceNotFoundException.java) | Scenario 7A | Thrown when database entity lookup fails |
| [`BusinessRuleException.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/exception/BusinessRuleException.java) | Scenario 7B | Thrown on domain violation; triggers transaction rollback |
| 13 JPA Entities (`Shop`, `AppUser`, etc.) | Scenario 1, 2, 4, 5, 6 | Map relational tables to Java objects |
| 12 JPA Repositories (`ShopRepository`, etc.) | Scenario 1, 2, 3, 4, 5, 6 | Execute SQL queries against PostgreSQL |
| 3 Flyway Migrations (`V1`, `V2`, `V3`) | Scenario 1 | Initialize database schema, defaults, and delivery extensions |
| Templates (`layout.html`, `login.html`, etc.) | Scenario 2, 3, 4, 7, 8 | Render server-side dynamic HTML views |
| Static Assets (`app.css`, `app.js`) | Scenario 2, 3, 4, 8 | Provide UI tokens, styling, CSRF headers, and alert timers |

---

## Scenario 1: Application Initialization & Cold Boot Flow

```mermaid
sequenceDiagram
    autonumber
    actor Admin as JVM / Developer
    participant Main as BackendApplication.main()
    participant Spring as SpringApplication.run()
    participant Config as AppConfig & WebMvcConfig
    participant Flyway as Flyway Migration Engine
    participant DB as PostgreSQL 17 Database
    participant JPA as Hibernate JPA Validator
    participant Sec as SecurityConfig FilterChain
    participant Tomcat as Embedded Tomcat Server

    Admin->>Main: java -jar backend.jar OR mvn spring-boot:run
    Main->>Spring: SpringApplication.run(BackendApplication.class, args)
    Spring->>Config: Initialize AppConfig -> Register Clock Bean (Asia/Kolkata)
    Spring->>Config: Initialize WebMvcConfig -> Map /uploads/** to local disk
    Spring->>Flyway: Scan classpath:db/migration/
    Flyway->>DB: Query flyway_schema_history table
    alt Migrations Pending
        Flyway->>DB: Execute V1__core_schema.sql (11 tables + sequence order_no_seq)
        Flyway->>DB: Execute V2__seed_defaults.sql (Shirt, Pant, Kurta, Blouse + 25 fields)
        Flyway->>DB: Execute V3__stage2_delivery.sql (order_item status + delivery_override table)
        Flyway->>DB: Record applied migration checksums in flyway_schema_history
    else Schema Already Up to Date
        Flyway-->>Spring: Schema verified at version 3
    end
    Spring->>JPA: Validate @Entity mappings against PostgreSQL tables (ddl-auto=validate)
    JPA->>DB: Inspect table columns, types, and constraints
    JPA-->>Spring: All 13 Entities validated successfully
    Spring->>Sec: Build SecurityFilterChain (formLogin, CSRF, Route Matchers)
    Spring->>Tomcat: Start HTTP Web Server on port 8080
    Tomcat-->>Admin: Tomcat started on port 8080. Application ready.
```

---

## Scenario 2: First-Run Shop Creation & Owner Registration (`GET & POST /signup`)

```mermaid
flowchart TD
    Start(["Visitor navigates to GET /signup"]) --> SecFilter{"SecurityConfig<br>Request Whitelist Check"}
    SecFilter -->|Permitted| AuthCtrlGet["AuthController.showSignupPage()"]
    AuthCtrlGet --> FirstRunCheck{"AuthService.isFirstRun()<br>appUserRepository.count() == 0?"}
    
    FirstRunCheck -->|Yes: Fresh Database| ShowFirstRun["Model: isFirstRun = true<br>Form pre-populated with shop name<br>Render auth/signup.html"]
    FirstRunCheck -->|No: Users Exist| CheckAuth{"Is visitor authenticated & Role == OWNER?"}
    CheckAuth -->|No| RedirectLogin["Redirect to GET /login"]
    CheckAuth -->|Yes| ShowAddStaff["Model: isFirstRun = false<br>Render auth/signup.html (Add Staff Mode)"]

    ShowFirstRun --> UserSubmits["User fills form and clicks 'Create Shop & Owner'<br>POST /signup (fullName, mobile, password, shopName)"]
    UserSubmits --> PostSecFilter{"SecurityConfig<br>POST permit check"}
    PostSecFilter --> AuthCtrlPost["AuthController.handleSignup()"]
    AuthCtrlPost --> CanAccessPost{"AuthService.canAccessSignup(auth)"}
    CanAccessPost -->|Forbidden| RedirectLogin
    CanAccessPost -->|Permitted| DTOValidation{"Jakarta Validation<br>@Valid SignupForm"}
    
    DTOValidation -->|Errors Found (e.g. 8-digit mobile)| ShowFormErrors["bindingResult holds errors<br>Re-render auth/signup.html with inline red alerts"]
    DTOValidation -->|Valid Inputs| CheckMobile{"AuthService.isMobileRegistered(mobile)"}
    
    CheckMobile -->|Mobile Already Taken| MobileDuplicate["bindingResult.rejectValue('mobile', ...)<br>Re-render auth/signup.html"]
    CheckMobile -->|Mobile Available| FirstRunBranch{"isFirstRun()?"}
    
    FirstRunBranch -->|Yes| RegisterOwner["AuthService.registerFirstOwner(form)<br>@Transactional Boundary Begins"]
    RegisterOwner --> CheckShop{"ShopRepository.findAll() empty?"}
    CheckShop -->|Yes| CreateShop["ShopRepository.save(new Shop('Geetanjali Tailors'))"]
    CheckShop -->|No| UseShop["Use existing Shop entity"]
    CreateShop --> HashPassword["BCryptPasswordEncoder.encode(form.getPassword())"]
    UseShop --> HashPassword
    HashPassword --> CreateOwnerUser["AppUserRepository.save(new AppUser(shop, name, mobile, hash, Role.OWNER))"]
    CreateOwnerUser --> CommitTx["@Transactional Commits Database Transaction"]
    CommitTx --> SetFlashMsg["redirectAttributes.addFlashAttribute('successMessage', 'Shop setup complete!')"]
    SetFlashMsg --> RedirectLoginSuccess["Redirect to GET /login?signupSuccess"]
    RedirectLoginSuccess --> ShowLoginPage["AuthController.showLoginPage()<br>Renders auth/login.html with green success banner"]
```

---

## Scenario 3: Staff / Owner Form Login Lifecycle (`GET & POST /login`)

```mermaid
flowchart TD
    UserNav["User navigates to GET /login"] --> CheckFresh{"AuthService.isFirstRun()?"}
    CheckFresh -->|Yes: 0 Users in DB| RedirectSignup["Redirect to GET /signup (Force initial setup)"]
    CheckFresh -->|No: Shop Initialized| CheckAlreadyLoggedIn{"Is user already authenticated?"}
    
    CheckAlreadyLoggedIn -->|Yes| RedirectHome["Redirect to GET / (Home Dashboard)"]
    CheckAlreadyLoggedIn -->|No| RenderLogin["AuthController.showLoginPage()<br>Renders auth/login.html with mobile & password inputs"]

    RenderLogin --> SubmitLogin["User enters 10-digit mobile & password<br>Submits POST /login with hidden _csrf token"]
    SubmitLogin --> CsrfCheck{"SecurityConfig Filter<br>CSRF Token Valid?"}
    CsrfCheck -->|Invalid / Missing| Error403["Reject request: HTTP 403 Forbidden"]
    CsrfCheck -->|Valid| AuthFilter["UsernamePasswordAuthenticationFilter"]
    
    AuthFilter --> LoadUser["AppUserDetailsService.loadUserByUsername(mobile)"]
    LoadUser --> DBQuery{"AppUserRepository.findByMobile(mobile)"}
    
    DBQuery -->|User Not Found| UserNotFound["Throws UsernameNotFoundException"]
    UserNotFound --> AuthFailure["Authentication Failure Handler<br>Redirect to GET /login?error"]
    AuthFailure --> RenderLoginError["AuthController renders auth/login.html with red banner:<br>'Invalid mobile number or password'"]

    DBQuery -->|AppUser Found| WrapPrincipal["Wrap AppUser into AppUserDetails principal"]
    WrapPrincipal --> CheckBCrypt{"PasswordEncoder.matches(rawPassword, storedBCryptHash)"}
    CheckBCrypt -->|Password Mismatch| AuthFailure
    CheckBCrypt -->|Password Matches| AuthSuccess["Authentication Success Handler"]
    
    AuthSuccess --> CreateSession["Create HttpSession<br>Store SecurityContext with AppUserDetails principal<br>Set JSESSIONID cookie"]
    CreateSession --> RedirectToDashboard["Redirect to GET / (defaultSuccessUrl)"]
```

---

## Scenario 4: Authenticated Dashboard Request & Global Pre-processing (`GET /`)

```mermaid
sequenceDiagram
    autonumber
    actor Tailor as Authenticated Tailor / Owner
    participant Browser as Web Browser
    participant Sec as SecurityConfig Filter Chain
    participant Advice as GlobalModelAdvice (@ControllerAdvice)
    participant ShopSvc as ShopService
    participant ShopRepo as ShopRepository
    participant HomeCtrl as HomeController
    participant Template as home/index.html & fragments/layout.html

    Tailor->>Browser: Navigates to http://localhost:8080/
    Browser->>Sec: GET / with Cookie: JSESSIONID=...
    Sec->>Sec: Validate Session SecurityContext (User is Authenticated)
    Sec->>Advice: Trigger @ControllerAdvice before controller method runs
    Advice->>ShopSvc: shopName() -> shopService.getShopName()
    ShopSvc->>ShopRepo: findAll()
    ShopRepo-->>ShopSvc: [Shop(id=1, name="Geetanjali Tailors")]
    ShopSvc-->>Advice: "Geetanjali Tailors"
    Advice->>Advice: Extract @AuthenticationPrincipal AppUserDetails
    Advice->>Advice: Extract request.getRequestURI() -> "/"
    Advice-->>HomeCtrl: Populates Model: shopName, currentUser, currentUserName, isOwner, currentUri
    HomeCtrl->>HomeCtrl: showHomePage()
    HomeCtrl-->>Template: Returns view name "home/index"
    Template->>Template: Evaluate Thymeleaf Layout Fragments:
    Note over Template: top-bar displays: "Geetanjali Tailors", "Tailor: Ketan (OWNER)"
    Note over Template: bottom-bar highlights Home tab as active
    Template-->>Browser: HTTP 200 OK with fully rendered HTML
    Browser-->>Tailor: Displays Dashboard UI with Welcome Banner and Quick Action buttons
```

---

## Scenario 5: Authenticated Owner Adding a Staff Tailor (`POST /signup`)

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Authenticated Shop Owner
    participant Browser as Web Browser
    participant Sec as SecurityConfig Filter
    participant Ctrl as AuthController.handleSignup()
    participant Svc as AuthService.registerStaff()
    participant Encoder as PasswordEncoder (BCrypt)
    participant UserRepo as AppUserRepository
    participant DB as PostgreSQL 17

    Owner->>Browser: Enters staff details: Name="Ramesh Tailor", Mobile="9123456789", Password="secret", Role=TAILOR
    Browser->>Sec: POST /signup (with _csrf token and JSESSIONID cookie)
    Sec->>Sec: Verify CSRF token and Authenticated Session
    Sec->>Ctrl: handleSignup(form, bindingResult, auth, model, redirectAttributes)
    Ctrl->>Svc: canAccessSignup(auth)
    Svc->>Svc: Inspects auth.getAuthorities() -> Has "ROLE_OWNER"
    Svc-->>Ctrl: true (Access Granted)
    Ctrl->>Svc: isMobileRegistered("9123456789")
    Svc->>UserRepo: existsByMobile("9123456789")
    UserRepo->>DB: SELECT count(*) > 0 FROM app_user WHERE mobile = '9123456789'
    DB-->>UserRepo: false
    UserRepo-->>Svc: false
    Svc-->>Ctrl: false (Mobile is Unique)
    Ctrl->>Svc: registerStaff(form, currentShop)
    Note over Svc: @Transactional Boundary Starts
    Svc->>Encoder: encode("secret")
    Encoder-->>Svc: "$2a$10$wK4... (hashed password)"
    Svc->>UserRepo: save(new AppUser(currentShop, "Ramesh Tailor", "9123456789", hash, Role.TAILOR))
    UserRepo->>DB: INSERT INTO app_user (shop_id, full_name, mobile, password_hash, role) VALUES (...)
    DB-->>UserRepo: Inserted row id=3
    UserRepo-->>Svc: Saved AppUser entity
    Note over Svc: @Transactional Commits Atomically
    Svc-->>Ctrl: Saved AppUser
    Ctrl->>Ctrl: redirectAttributes.addFlashAttribute("successMessage", "Staff member Ramesh Tailor created successfully!")
    Ctrl-->>Browser: HTTP 302 Redirect to /?staffCreated
    Browser->>Sec: GET /?staffCreated
    Sec-->>Browser: HTTP 200 OK (home/index.html with green success alert banner)
    Browser-->>Owner: Screen displays: "Staff member Ramesh Tailor created successfully!"
```

---

## Scenario 6: Customer Cloth Photo Upload Flow

```mermaid
sequenceDiagram
    autonumber
    actor Tailor as Staff Tailor
    participant Browser as Web Browser
    participant Svc as OrderWizardService / PhotoService
    participant Storage as LocalDiskPhotoStorageService
    participant Disk as Local Server Filesystem (uploads/)
    participant PhotoRepo as ItemPhotoRepository
    participant DB as PostgreSQL 17
    participant WebMvc as WebMvcConfig Resource Handler

    Tailor->>Browser: Selects file "pattern.jpg" (size < 5MB) in order wizard
    Browser->>Svc: POST /orders/draft/photo with MultipartFile
    Svc->>Storage: store(multipartFile)
    Storage->>Storage: Check file not empty and extract extension (.jpg)
    Storage->>Storage: Generate UUID filename: "e4d909c2-....jpg"
    Storage->>Storage: Verify destinationPath.getParent().equals(rootLocation) [Anti-Traversal]
    Storage->>Disk: Files.copy(inputStream, destinationPath, REPLACE_EXISTING)
    Disk-->>Storage: File written to /home/shubham/.../uploads/e4d909c2-....jpg
    Storage-->>Svc: Returns web URL: "/uploads/e4d909c2-....jpg"
    Svc->>PhotoRepo: save(new ItemPhoto(orderItem, "/uploads/e4d909c2-....jpg", publicId))
    PhotoRepo->>DB: INSERT INTO item_photo (order_item_id, url, public_id) VALUES (...)
    DB-->>PhotoRepo: Saved
    Svc-->>Browser: Returns updated order draft with photo URL
    Browser->>WebMvc: GET /uploads/e4d909c2-....jpg
    WebMvc->>Disk: Read file from disk
    Disk-->>WebMvc: Binary image stream
    WebMvc-->>Browser: HTTP 200 OK (image/jpeg)
    Browser-->>Tailor: Displays photo thumbnail in the garment order item card
```

---

## Scenario 7: Global Exception & Error Handling Scenarios

### Scenario 7A: Non-Existent Route / Missing Resource (404 Not Found)

```mermaid
flowchart TD
    ReqMissing["User requests GET /non-existent-page"] --> SecCheck{"SecurityConfig Filter"}
    SecCheck -->|Authenticated / Public| Dispatcher["DispatcherServlet"]
    Dispatcher --> RouteMatch{"Handler Mapping Found?"}
    
    RouteMatch -->|No| BasicError["Forward to /error with HTTP 404"]
    BasicError --> ModelAdvice["GlobalModelAdvice injects shopName & currentUser"]
    ModelAdvice --> Render404["Renders error/404.html<br>HTTP Status Code: 404 Not Found<br>Displays shop header and 'Return to Home' button"]
    
    RouteMatch -->|Yes: Controller executes| SvcCall["Service executes entity lookup"]
    SvcCall --> EntityCheck{"entityRepository.findById(id) exists?"}
    EntityCheck -->|No| Throw404["throw new ResourceNotFoundException('Customer with ID 42 not found')"]
    Throw404 --> ExHandler404["GlobalExceptionHandler.handleResourceNotFound(ex, model)"]
    ExHandler404 --> SetErrModel["model.addAttribute('errorMessage', ex.getMessage())"]
    SetErrModel --> Render404
```

### Scenario 7B: Business Rule Violation (302 Flash Redirect)

```mermaid
flowchart TD
    SvcAction["Service method runs inside @Transactional<br>(e.g. deliver garment with unpaid balance without override)"] --> ValidateRule{"Domain Rule Check:<br>balance > 0 AND override == null?"}
    
    ValidateRule -->|Rule Broken| ThrowBusinessEx["throw new BusinessRuleException('Cannot deliver garment with unpaid balance of ₹450 without owner override')"]
    ThrowBusinessEx --> RollbackTx["@Transactional detects unchecked RuntimeException<br>ROLLS BACK all database modifications in this transaction"]
    RollbackTx --> ExHandlerBiz["GlobalExceptionHandler.handleBusinessRule(ex, request, redirectAttributes)"]
    
    ExHandlerBiz --> AddFlash["redirectAttributes.addFlashAttribute('errorMessage', ex.getMessage())<br>(Saved in HttpSession for exactly ONE redirect)"]
    AddFlash --> ReadReferer["String referer = request.getHeader('Referer')"]
    ReadReferer --> CheckReferer{"referer != null && !referer.isBlank()?"}
    
    CheckReferer -->|Yes| RedirectReferer["Returns 'redirect:' + referer"]
    CheckReferer -->|No| RedirectHomeFallback["Returns 'redirect:/'"]
    
    RedirectReferer --> BrowserReload["Browser issues GET request to previous page"]
    RedirectHomeFallback --> BrowserReload
    BrowserReload --> LayoutRender["fragments/layout.html evaluates flash-messages block:<br>Displays red alert banner with error message"]
```

---

## Scenario 8: Safe Logout Flow with CSRF Verification (`POST /logout`)

```mermaid
sequenceDiagram
    autonumber
    actor Tailor as Logged-in Staff Member
    participant View as fragments/layout.html
    participant Sec as SecurityConfig LogoutFilter
    participant Session as HTTP Session Store
    participant Browser as Web Browser

    Tailor->>View: Clicks "Logout" button in top-bar
    View->>Sec: Submits form POST /logout with hidden _csrf token
    Sec->>Sec: Verify CSRF token matches session token
    alt CSRF Valid
        Sec->>Session: Invalidate active HttpSession
        Sec->>Session: Clear SecurityContext and Authentication Principal
        Sec->>Browser: Set-Cookie: JSESSIONID=; Max-Age=0 (Deletes Cookie)
        Sec-->>Browser: HTTP 302 Redirect to /login?logout
        Browser->>Browser: Issues GET /login?logout
        Browser-->>Tailor: Renders auth/login.html with green banner:<br>"You have been logged out successfully."
    else CSRF Invalid / Missing
        Sec-->>Browser: HTTP 403 Forbidden (CSRF attack prevented)
    end
```
