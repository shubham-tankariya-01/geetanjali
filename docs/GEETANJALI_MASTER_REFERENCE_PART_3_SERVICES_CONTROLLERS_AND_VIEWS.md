# Geetanjali Tailor Shop System — Master Reference (Part 3: Services, Controllers & Views)

> **Document Scope**: Part 3 of 4 — Deep dive into Services, Controllers, Global Advice, Flyway Migrations, Thymeleaf Templates, and Static CSS/JS Assets.  
> **Project**: Geetanjali Tailor Shop Management Web Application  
> **Target Audience**: Student pair programmer learning Spring Boot during a 24-hour hackathon.  
> **Version / Git Commit**: `Stage 1 - Step 1: Shared foundation` (`1d14e6f`)

---

## 1. Service Layer Breakdown (`service/`, `service/storage/`)

Services contain all business rules, calculations, and transactional boundaries (`@Transactional`). Controllers must never bypass services to access repositories directly.

---

### Service 1: [`AuthService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/AuthService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/AuthService.java`
* **Real-Life Analogy**: The business office manager who checks identity documents, encrypts contracts, and officially registers employees.
* **Class Explanation**:
  * Marked with `@Service`. Encapsulates all business logic for registration, authentication checks, and user creation.
* **Injected Dependencies**:
  * `private final AppUserRepository appUserRepository`
  * `private final ShopRepository shopRepository`
  * `private final PasswordEncoder passwordEncoder`
* **Functions & Methods**:

  1. `public AuthService(AppUserRepository appUserRepository, ShopRepository shopRepository, PasswordEncoder passwordEncoder)`
     * **Signature**: Takes 3 injected beans.
     * **Returns**: Initialized `AuthService` singleton.

  2. `public boolean isFirstRun()`
     * **Signature**: Takes no parameters.
     * **In-Built Functions Used**: `appUserRepository.count() == 0` — Executes SQL `SELECT count(*) FROM app_user`.
     * **Returns**: `boolean` (`true` if database has 0 users, meaning initial shop owner setup is required; `false` otherwise).
     * **Caller / Destination**: Called by `AuthController.showLoginPage()` (to redirect to `/signup`) and `canAccessSignup()`.

  3. `public boolean isMobileRegistered(String mobile)`
     * **Signature**: Takes `mobile` (`String`).
     * **In-Built Functions Used**: `appUserRepository.existsByMobile(mobile)` — Executes SQL `SELECT count(*) > 0 FROM app_user WHERE mobile = ?`.
     * **Returns**: `boolean` (`true` if mobile exists in PostgreSQL).
     * **Caller / Destination**: Called by `AuthController.handleSignup()` to reject duplicate mobile numbers.

  4. `public boolean canAccessSignup(Authentication auth)`
     * **Signature**: Takes `auth` (`Authentication`, Spring Security principal).
     * **In-Built Functions Used**: `auth.getAuthorities()` — Iterates user's granted authorities.
     * **Flow & Logic**:
       * If `isFirstRun()` is `true` -> returns `true` (public registration permitted for shop owner).
       * If `auth == null` or `!auth.isAuthenticated()` -> returns `false`.
       * Loops through authorities: if any authority matches `"ROLE_OWNER"` -> returns `true`.
       * Otherwise -> returns `false` (regular tailors or guests cannot add staff).
     * **Returns**: `boolean`.
     * **Caller / Destination**: Called by `AuthController.showSignupPage()` and `AuthController.handleSignup()`.

  5. `public AppUser registerFirstOwner(SignupForm form)`
     * **Annotation**: `@Transactional` — Wraps database operations in a single atomic transaction; automatically rolls back if an error occurs.
     * **Signature**: Takes `form` (`SignupForm`, validated form DTO).
     * **In-Built Functions Used**:
       * `shopRepository.findAll()`: Checks if a default shop row exists.
       * `passwordEncoder.encode(form.getPassword())`: Uses BCrypt to hash the plain text password.
       * `shopRepository.save(shop)`: Saves new `Shop` entity if none existed.
       * `appUserRepository.save(owner)`: Saves new `AppUser` entity with `Role.OWNER`.
     * **Returns**: `AppUser` — The newly created owner entity.
     * **Caller / Destination**: Called by `AuthController.handleSignup()` during first-run setup.

  6. `public AppUser registerStaff(SignupForm form, Shop currentShop)`
     * **Annotation**: `@Transactional`.
     * **Signature**: Takes `form` (`SignupForm`), `currentShop` (`Shop`).
     * **In-Built Functions Used**:
       * `passwordEncoder.encode(form.getPassword())`: Hashes staff password.
       * `appUserRepository.save(staff)`: Saves new `AppUser` entity.
     * **Returns**: `AppUser` — The newly created staff member.
     * **Caller / Destination**: Called by `AuthController.handleSignup()` when an authenticated owner adds a tailor.

---

### Service 2: [`ShopService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/ShopService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/ShopService.java`
* **Real-Life Analogy**: The shop information desk providing the shop name and store profile.
* **Class Explanation**:
  * Marked with `@Service`. Handles queries and operations for the shop profile.
* **Injected Dependencies**:
  * `private final ShopRepository shopRepository`
* **Functions & Methods**:

  1. `public ShopService(ShopRepository shopRepository)` — Constructor injection.
  2. `public Shop getPrimaryShop()`
     * **Signature**: Takes no parameters.
     * **In-Built Functions Used**: `shopRepository.findAll()`.
     * **Returns**: `Shop` entity, or `null` if database is uninitialized.
     * **Caller / Destination**: Called by `getShopName()` and controllers.
  3. `public String getShopName()`
     * **Signature**: Takes no parameters.
     * **In-Built Functions Used**: `getPrimaryShop()`.
     * **Returns**: `String` — Shop display name (e.g. `"Geetanjali Tailors"`), or fallback `"Geetanjali Tailors"` if uninitialized.
     * **Caller / Destination**: Called by `GlobalModelAdvice.shopName()` to populate `${shopName}` in all views.

---

### Service 3: [`PhotoStorageService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/storage/PhotoStorageService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/storage/PhotoStorageService.java`
* **Real-Life Analogy**: The storage locker contract defining how cloth photos are stored and retrieved.
* **Class Explanation**: Interface abstracting file storage operations to allow future swapping with Cloudinary/S3.
* **Methods**:
  1. `String store(MultipartFile file)` — Saves uploaded file and returns web-accessible URL.
  2. `void delete(String identifier)` — Deletes photo by URL or file ID.

---

### Service 4: [`LocalDiskPhotoStorageService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/storage/LocalDiskPhotoStorageService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/storage/LocalDiskPhotoStorageService.java`
* **Real-Life Analogy**: The local file folder storage system on the shop computer.
* **Class Explanation**: Marked with `@Service`. Implements `PhotoStorageService` by writing files to local disk under `uploads/` with random UUID filenames.
* **Fields**:
  * `private final Path rootLocation` — Absolute disk path for `uploads/`.
* **Functions & Methods**:

  1. `public LocalDiskPhotoStorageService(@Value("${app.upload-dir:uploads}") String uploadDir)`
     * **Signature**: Takes `uploadDir` (`String`) from property `app.upload-dir`.
     * **In-Built Functions Used**:
       * `Paths.get(uploadDir).toAbsolutePath().normalize()`: Resolves absolute disk path.
       * `Files.createDirectories(this.rootLocation)`: Creates `uploads/` folder if it doesn't exist.
     * **Returns**: Initialized service bean.

  2. `public String store(MultipartFile file)`
     * **Annotation**: `@Override`
     * **Signature**: Takes `file` (`MultipartFile`, uploaded binary file).
     * **In-Built Functions Used**:
       * `UUID.randomUUID().toString()`: Generates unique filename to prevent overwrites.
       * `destinationFile.getParent().equals(this.rootLocation)`: Security check preventing path traversal attacks.
       * `Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING)`: Streams binary data to disk.
     * **Throws**:
       * `IllegalArgumentException` if file is null or empty.
       * `SecurityException` if path traversal detected.
       * `RuntimeException` if disk write fails.
     * **Returns**: `String` URL path (e.g. `"/uploads/7b41e8c1-....jpg"`).
     * **Caller / Destination**: Called during cloth photo upload in order creation wizard.

  3. `public void delete(String identifier)`
     * **Annotation**: `@Override`
     * **Signature**: Takes `identifier` (`String`, URL or filename).
     * **In-Built Functions Used**: `Files.deleteIfExists(path)`.
     * **Returns**: `void`.
     * **Caller / Destination**: Called when order items or photos are removed.

---

### Service 5: [`GarmentService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/GarmentService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/GarmentService.java`
* **Real-Life Analogy**: The garment catalog manager and measurement pattern binder who knows all available clothing types and their required measurement boxes.
* **Class Explanation**:
  * Marked with `@Service` and `@Transactional(readOnly = true)`.
  * Provides query operations for active garments and their configured measurement template fields.
* **Injected Dependencies**:
  * `private final GarmentRepository garmentRepository`
  * `private final GarmentFieldRepository garmentFieldRepository`
* **Functions & Methods**:

  1. `public GarmentService(GarmentRepository garmentRepository, GarmentFieldRepository garmentFieldRepository)`
     * **Signature**: Takes `garmentRepository` (`GarmentRepository`), `garmentFieldRepository` (`GarmentFieldRepository`).
     * **Returns**: Initialized `GarmentService` singleton bean.

  2. `public List<Garment> listActiveGarments()`
     * **Signature**: Takes no parameters.
     * **In-Built Functions Used**: `garmentRepository.findByActiveTrueOrderBySortOrderAsc()`.
     * **Returns**: `List<Garment>` — All active garments sorted by display sort order (e.g., Shirt, Pant, Kurta, Blouse).
     * **Caller / Destination**: Order creation wizard to render garment category selection tiles.

  3. `public Garment getGarmentById(Long id)`
     * **Signature**: Takes `id` (`Long`) — Garment primary key.
     * **In-Built Functions Used**: `garmentRepository.findById(id).orElseThrow(...)`.
     * **Throws**: `ResourceNotFoundException` if no garment with this ID exists.
     * **Returns**: `Garment` entity.
     * **Caller / Destination**: Called by `getActiveFields()` and order management controllers.

  4. `public List<GarmentField> getActiveFields(Long garmentId)`
     * **Signature**: Takes `garmentId` (`Long`).
     * **In-Built Functions Used**:
       * `getGarmentById(garmentId)`: Verifies garment existence, throwing `ResourceNotFoundException` if unknown.
       * `garmentFieldRepository.findByGarmentIdAndActiveTrueOrderBySortOrderAsc(garmentId)`: Fetches active fields in ascending sort order.
     * **Returns**: `List<GarmentField>` — Active measurement boxes (Chest, Waist, Inseam, etc.) for that garment.
     * **Caller / Destination**: Measurement recording screens and order item customization.

---

### Service 6: [`CustomerService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/CustomerService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/CustomerService.java`
* **Real-Life Analogy**: The customer ledger keeper who searches the archives by phone or name, counts past visits, and checks the date of their last suit order.
* **Class Explanation**:
  * Marked with `@Service` and `@Transactional(readOnly = true)`.
  * Encapsulates all read queries, search logic, mobile normalization, and order statistics for customers.
* **Injected Dependencies**:
  * `private final CustomerRepository customerRepository`
  * `private final CustomerOrderRepository customerOrderRepository`
  * `private final Clock clock` (Asia/Kolkata IST clock from `AppConfig`)
* **Functions & Methods**:

  1. `public CustomerService(CustomerRepository customerRepository, CustomerOrderRepository customerOrderRepository, Clock clock)`
     * **Signature**: Constructor injection with 3 dependencies.
     * **Returns**: Initialized `CustomerService` singleton bean.

  2. `public List<Customer> search(String query)`
     * **Signature**: Takes `query` (`String`) — Search keyword (name or phone digits).
     * **In-Built Functions Used**:
       * `customerRepository.findAllByOrderByNameAsc()`: If `query` is null or blank, returns all customers sorted alphabetically.
       * `customerRepository.searchByNameOrMobile(cleaned)`: Bound parameter JPQL query with `LOWER()` case-insensitivity.
     * **Returns**: `List<Customer>` — Matching customers ordered alphabetically by name.
     * **Caller / Destination**: Called by `CustomerController.listCustomers()`.

  3. `public Optional<Customer> findByMobile(String mobile)`
     * **Signature**: Takes `mobile` (`String`) — Raw phone number.
     * **In-Built Functions Used**:
       * `PhoneNumbers.normalize(mobile)`: Strips `+91`, spaces, hyphens, and trunk zeros into clean 10 digits.
       * `customerRepository.findByMobile(normalized)`: Exact database lookup.
     * **Returns**: `Optional<Customer>` — Wrapped customer entity or empty if not found.
     * **Caller / Destination**: Called by `CustomerApiController.lookupCustomer()`.

  4. `public Customer getById(Long id)`
     * **Signature**: Takes `id` (`Long`) — Customer primary key.
     * **In-Built Functions Used**: `customerRepository.findById(id).orElseThrow(...)`.
     * **Throws**: `ResourceNotFoundException` if customer does not exist (triggers 404 response).
     * **Returns**: `Customer` entity.
     * **Caller / Destination**: Called by `CustomerController.showCustomerDetail()`.

  5. `public long countEarlierOrders(Long customerId)`
     * **Signature**: Takes `customerId` (`Long`).
     * **In-Built Functions Used**: `customerOrderRepository.countByCustomerId(customerId)`.
     * **Returns**: `long` — Total count of earlier orders.
     * **Caller / Destination**: Customer profile view and `CustomerApiController.lookupCustomer()`.

  6. `public Optional<LocalDate> findLastOrderDate(Long customerId)`
     * **Signature**: Takes `customerId` (`Long`).
     * **In-Built Functions Used**:
       * `customerOrderRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId)`: Fetches latest order.
       * `order.getCreatedAt().atZone(clock.getZone()).toLocalDate()`: Converts UTC Instant into local Indian Standard Time date.
     * **Returns**: `Optional<LocalDate>` — Date of most recent order, or empty if customer has never ordered.
     * **Caller / Destination**: Customer profile view and `CustomerApiController.lookupCustomer()`.

---

## 2. Controller & Advice Layer Breakdown (`controller/`, `controller/advice/`, `controller/api/`)

---

### Controller 1: [`GlobalModelAdvice.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/advice/GlobalModelAdvice.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/advice/GlobalModelAdvice.java`
* **Real-Life Analogy**: The shop assistant who hands every customer a branded header card and stamps their session receipt before they look at any garment.
* **Class Explanation**: Marked with `@ControllerAdvice`. Injects common attributes into every single Thymeleaf template automatically.
* **Injected Dependencies**:
  * `private final ShopService shopService`
* **Functions & Methods**:

  1. `public GlobalModelAdvice(ShopService shopService)` — Constructor injection.
  2. `public String shopName()`
     * **Annotation**: `@ModelAttribute("shopName")`
     * **Signature**: Takes no parameters.
     * **In-Built Functions Used**: `shopService.getShopName()`.
     * **Returns**: `String` — Shop display name to template context `${shopName}`.
  3. `public AppUserDetails currentUser(@AuthenticationPrincipal AppUserDetails userDetails)`
     * **Annotation**: `@ModelAttribute("currentUser")`
     * **Signature**: Takes `userDetails` (`AppUserDetails`, resolved from Spring Security context).
     * **Returns**: `AppUserDetails` (or `null` if guest) to template context `${currentUser}`.
  4. `public String currentUserName(@AuthenticationPrincipal AppUserDetails userDetails)`
     * **Annotation**: `@ModelAttribute("currentUserName")`
     * **Signature**: Takes `userDetails` (`AppUserDetails`).
     * **Returns**: User's full name (`String`) or `""` to template context `${currentUserName}`.
  5. `public boolean isOwner(@AuthenticationPrincipal AppUserDetails userDetails)`
     * **Annotation**: `@ModelAttribute("isOwner")`
     * **Signature**: Takes `userDetails` (`AppUserDetails`).
     * **Returns**: `boolean` (`true` if logged in and role is `Role.OWNER`) to template context `${isOwner}`.
  6. `public String currentUri(HttpServletRequest request)`
     * **Annotation**: `@ModelAttribute("currentUri")`
     * **Signature**: Takes `request` (`HttpServletRequest`).
     * **In-Built Functions Used**: `request.getRequestURI()`.
     * **Returns**: `String` (e.g. `"/"`, `"/customers"`) to template context `${currentUri}` for navbar active-tab highlighting.

---

### Controller 2: [`GlobalExceptionHandler.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/advice/GlobalExceptionHandler.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/advice/GlobalExceptionHandler.java`
* **Real-Life Analogy**: The store manager who steps in when something goes missing or an invalid order is attempted, calming the situation without crashing the store.
* **Class Explanation**: Marked with `@ControllerAdvice`. Catches unhandled exceptions thrown across all controllers.
* **Functions & Methods**:

  1. `public String handleResourceNotFound(ResourceNotFoundException ex, Model model)`
     * **Annotations**: `@ExceptionHandler(ResourceNotFoundException.class)`, `@ResponseStatus(HttpStatus.NOT_FOUND)`
     * **Signature**: Takes `ex` (`ResourceNotFoundException`), `model` (`Model`).
     * **In-Built Functions Used**: `model.addAttribute("errorMessage", ex.getMessage())`.
     * **Returns**: `String` view name `"error/404"`.
     * **Caller / Destination**: Renders `error/404.html` with HTTP 404 status.

  2. `public String handleBusinessRule(BusinessRuleException ex, HttpServletRequest request, RedirectAttributes redirectAttributes)`
     * **Annotation**: `@ExceptionHandler(BusinessRuleException.class)`
     * **Signature**: Takes `ex` (`BusinessRuleException`), `request` (`HttpServletRequest`), `redirectAttributes` (`RedirectAttributes`).
     * **In-Built Functions Used**:
       * `redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage())`: Stores error message in session for one redirect.
       * `request.getHeader("Referer")`: Reads browser's previous page URL.
     * **Returns**: `String` redirect instruction `"redirect:" + referer` (or fallback `"redirect:/"`).
     * **Caller / Destination**: Browser redirects back to previous page; `fragments/layout.html` displays red error banner.

---

### Controller 3: [`AuthController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/AuthController.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/AuthController.java`
* **Real-Life Analogy**: The shop's front desk receptionist handling staff sign-in and initial shop owner registration.
* **Class Explanation**: Marked with `@Controller`. Manages web endpoints for `/login` and `/signup`. Decoupled from repositories (uses `AuthService` and `ShopService`).
* **Injected Dependencies**:
  * `private final AuthService authService`
  * `private final ShopService shopService`
* **Functions & Methods**:

  1. `public AuthController(AuthService authService, ShopService shopService)` — Constructor injection.
  2. `public String showLoginPage(Authentication auth, String error, String logout, String signupSuccess, Model model)`
     * **Annotation**: `@GetMapping("/login")`
     * **Signature**: Takes `auth` (`Authentication`), query flags `error`, `logout`, `signupSuccess`, and `model` (`Model`).
     * **Flow & Logic**:
       * If `authService.isFirstRun()` -> returns `"redirect:/signup"`.
       * If user is already authenticated -> returns `"redirect:/"`.
       * Adds alert banners (`loginError`, `logoutMessage`, `successMessage`) to `model`.
     * **Returns**: `"auth/login"` template view name.
  3. `public String showSignupPage(Authentication auth, Model model)`
     * **Annotation**: `@GetMapping("/signup")`
     * **Signature**: Takes `auth` (`Authentication`), `model` (`Model`).
     * **Flow & Logic**:
       * Checks `authService.canAccessSignup(auth)`. If false, returns `"redirect:/login"`.
       * Checks `authService.isFirstRun()` and adds `isFirstRun` flag to `model`.
       * Binds empty `SignupForm` (pre-populated with shop name if first run).
     * **Returns**: `"auth/signup"` template view name.
  4. `public String handleSignup(SignupForm form, BindingResult bindingResult, Authentication auth, Model model, RedirectAttributes redirectAttributes)`
     * **Annotation**: `@PostMapping("/signup")`
     * **Signature**: Takes `form` (`@Valid @ModelAttribute("signupForm") SignupForm`), `bindingResult` (`BindingResult`), `auth` (`Authentication`), `model` (`Model`), `redirectAttributes` (`RedirectAttributes`).
     * **Flow & Logic**:
       * Verifies `authService.canAccessSignup(auth)`. If false, returns `"redirect:/login"`.
       * Checks `authService.isMobileRegistered(form.getMobile())`. If taken, adds duplicate mobile error to `bindingResult`.
       * If `bindingResult.hasErrors()` -> returns `"auth/signup"`.
       * If first run: calls `authService.registerFirstOwner(form)` and redirects to `"redirect:/login?signupSuccess"`.
       * If adding staff: extracts current shop from `auth`, calls `authService.registerStaff(form, shop)`, and redirects to `"redirect:/?staffCreated"`.
     * **Returns**: Redirect string or template path.

---

### Controller 4: [`HomeController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/HomeController.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/HomeController.java`
* **Real-Life Analogy**: The shop's main showroom lobby.
* **Class Explanation**: Marked with `@Controller`. Handles `GET /`.
* **Functions & Methods**:
  1. `public String showHomePage()`
     * **Annotation**: `@GetMapping("/")`
     * **Signature**: Takes no parameters.
     * **Returns**: `"home/index"` template view name.

---

### Record DTO: [`CustomerLookupResponse.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/api/CustomerLookupResponse.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/api/CustomerLookupResponse.java`
* **Real-Life Analogy**: The quick summary receipt handed across the counter stating whether a customer exists, their name, how many suits they've stitched before, and their last visit date.
* **Class Explanation**:
  * An immutable Java `record` used as the response body for `GET /api/customers/lookup?mobile=`.
  * Java records automatically generate private final fields, constructor, getters, `equals()`, `hashCode()`, and `toString()`.
  * Jackson automatically serializes this record into JSON `{ "found": true, "name": "...", "orderCount": 1, "lastOrderDate": "2026-10-12" }`.
* **Fields / Record Components**:
  * `boolean found` — `true` if customer exists in database, `false` otherwise.
  * `String name` — Customer full name (or `null` if not found).
  * `long orderCount` — Number of earlier orders placed (or `0` if not found).
  * `LocalDate lastOrderDate` — Date of most recent order (or `null` if no past orders).
* **Factory Methods**:
  1. `public static CustomerLookupResponse notFound()`: Returns `new CustomerLookupResponse(false, null, 0, null)`.
  2. `public static CustomerLookupResponse found(String name, long orderCount, LocalDate lastOrderDate)`: Returns populated response with `found = true`.

---

### Controller 5: [`CustomerApiController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/api/CustomerApiController.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/api/CustomerApiController.java`
* **Real-Life Analogy**: The express drive-thru window for instant data lookups without re-rendering the whole showroom.
* **Class Explanation**:
  * Marked with `@RestController` and `@RequestMapping("/api/customers")`.
  * Provides lightweight JSON endpoints intended for AJAX `fetch()` calls from the browser (e.g., during the order creation wizard).
  * **WHY `@RestController` vs `@Controller`**:
    * `@Controller` is designed for human web browsing; its handler methods return view name strings (like `"customers/index"`), which Thymeleaf resolves and merges with HTML.
    * `@RestController` combines `@Controller` and `@ResponseBody`. Instead of returning HTML, Spring's Jackson converter serializes the returned Java record directly into JSON and writes it into the HTTP response body. In Express, `@Controller` is `res.render('view')`, while `@RestController` is `res.json(data)`.
* **Injected Dependencies**:
  * `private final CustomerService customerService`
* **Functions & Methods**:

  1. `public CustomerApiController(CustomerService customerService)` — Constructor injection.
  2. `public CustomerLookupResponse lookupCustomer(@RequestParam("mobile") String mobile)`
     * **Annotation**: `@GetMapping("/lookup")`
     * **Signature**:
       * Takes: `mobile` (`@RequestParam("mobile") String`) — Mobile number query parameter from the URL.
     * **In-Built Functions Used**:
       * `PhoneNumbers.normalize(mobile)`: Cleans input (strips +91, dashes, spaces) before database query.
       * `customerService.findByMobile(normalized)`: Looks up customer in database.
       * `customerService.countEarlierOrders(id)`: Counts past orders.
       * `customerService.findLastOrderDate(id)`: Obtains last order date via IST `Clock`.
     * **Returns**: `CustomerLookupResponse` — Serialized directly to JSON by Spring MVC Jackson.
       * If found: `{ "found": true, "name": "Ramesh Patel", "orderCount": 1, "lastOrderDate": "2026-10-12" }`.
       * If not found: `{ "found": false, "name": null, "orderCount": 0, "lastOrderDate": null }`.
     * **Caller / Destination**: Client-side JavaScript (`fetch('/api/customers/lookup?mobile=...')`) or external API consumers.

---

### Controller 6: [`CustomerController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/CustomerController.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/CustomerController.java`
* **Real-Life Analogy**: The customer service counter where tailors view the customer directory and inspect individual profile cards.
* **Class Explanation**:
  * Marked with `@Controller` and `@RequestMapping("/customers")`.
  * Thin controller handling web UI routes for customer search, directory listing, and individual profile views. Never touches repositories directly.
* **Injected Dependencies**:
  * `private final CustomerService customerService`
* **Functions & Methods**:

  1. `public CustomerController(CustomerService customerService)` — Constructor injection.
  2. `public String listCustomers(@RequestParam(value = "q", required = false) String query, Model model)`
     * **Annotation**: `@GetMapping`
     * **Signature**:
       * Takes: `query` (`@RequestParam(value = "q", required = false) String`) — Optional search keyword.
       * Takes: `model` (`Model`) — View container.
     * **In-Built Functions Used**:
       * `customerService.search(query)`: Searches customers by name or phone, or lists all if blank.
       * `model.addAttribute("customers", customers)`: Injects customer list into template context.
       * `model.addAttribute("q", query != null ? query.trim() : "")`: Preserves active search term in input box.
     * **Returns**: `String` view name `"customers/index"`.
     * **Caller / Destination**: Browser loads `customers/index.html`.

  3. `public String showCustomerDetail(@PathVariable("id") Long id, Model model)`
     * **Annotation**: `@GetMapping("/{id}")`
     * **Signature**:
       * Takes: `id` (`@PathVariable("id") Long`) — Customer primary key from URL path.
       * Takes: `model` (`Model`) — View container.
     * **In-Built Functions Used**:
       * `customerService.getById(id)`: Fetches customer entity. If missing, throws `ResourceNotFoundException`, which `GlobalExceptionHandler` converts to a 404 page.
       * `customerService.countEarlierOrders(id)`: Queries total past order count.
       * `customerService.findLastOrderDate(id)`: Obtains last order date.
     * **Returns**: `String` view name `"customers/detail"`.
     * **Caller / Destination**: Browser loads `customers/detail.html`.

---

## 3. Flyway Database Migrations (`resources/db/migration/`)

Database migrations run sequentially on startup before Hibernate initializes. Once applied, migration files are immutable.

---

### Migration 1: [`V1__core_schema.sql`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/db/migration/V1__core_schema.sql)
* **Execution Order**: 1st migration.
* **Tables Created**:
  1. `shop` — Columns: `id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY`, `name VARCHAR(255) NOT NULL`.
  2. `app_user` — Columns: `id`, `shop_id FK`, `full_name`, `mobile UNIQUE`, `password_hash`, `role`.
  3. `customer` — Columns: `id`, `name`, `mobile UNIQUE`.
  4. `garment` — Columns: `id`, `name UNIQUE`, `sort_order`, `active`.
  5. `garment_field` — Columns: `id`, `garment_id FK`, `label`, `unit`, `sort_order`, `active`.
  6. `measurement` — Columns: `id`, `customer_id FK`, `garment_id FK`, `created_at TIMESTAMPTZ`.
  7. `measurement_value` — Columns: `measurement_id FK`, `field_id FK`, `value NUMERIC(10,2)`, `PRIMARY KEY (measurement_id, field_id)`.
  8. `customer_order` — Columns: `id`, `order_no BIGINT UNIQUE`, `customer_id FK`, `created_by FK`, `created_at TIMESTAMPTZ`.
  9. `order_item` — Columns: `id`, `order_id FK`, `garment_id FK`, `measurement_id FK`, `notes TEXT`, `price NUMERIC(10,2)`, `delivery_date DATE`.
  10. `item_photo` — Columns: `id`, `order_item_id FK`, `url VARCHAR(500)`, `public_id VARCHAR(255)`.
  11. `payment` — Columns: `id`, `order_id FK`, `amount NUMERIC(10,2)`, `mode VARCHAR(50)`, `paid_on DATE`.
* **Sequences Created**:
  * `order_no_seq START WITH 1001 INCREMENT BY 1` — Generates human-friendly sequential bill numbers.
* **Indexes Created**:
  * Foreign key indexes on all child tables to guarantee fast relational joins.

---

### Migration 2: [`V2__seed_defaults.sql`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/db/migration/V2__seed_defaults.sql)
* **Execution Order**: 2nd migration.
* **Seeds Standard Clothing Categories**:
  * `Shirt` (sort_order = 10)
  * `Pant` (sort_order = 20)
  * `Kurta` (sort_order = 30)
  * `Blouse` (sort_order = 40)
* **Seeds 25 Standard Measurement Fields**:
  * Shirt: Length, Chest, Waist, Shoulder, Sleeve Length, Collar, Cuff (all with unit `'inch'`).
  * Pant: Length, Waist, Hip, Inseam, Thigh, Bottom / Mohri.
  * Kurta: Length, Chest, Waist, Shoulder, Sleeve Length, Neck.
  * Blouse: Length, Bust, Under Bust, Shoulder, Sleeve Length, Front Neck Depth.

---

### Migration 3: [`V3__stage2_delivery.sql`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/db/migration/V3__stage2_delivery.sql)
* **Execution Order**: 3rd migration.
* **Enhancements to `order_item`**:
  * Adds `status VARCHAR(20) NOT NULL DEFAULT 'PENDING'`.
  * Adds `done_at TIMESTAMPTZ`.
  * Adds `delivered_at TIMESTAMPTZ`.
* **Creates Audit Table**:
  * `delivery_override` — Columns: `id`, `order_item_id FK`, `balance_at_time NUMERIC(10,2)`, `reason TEXT`, `overridden_by FK`.

---

## 4. Frontend Templates & Static Assets (`resources/templates/`, `resources/static/`)

---

### Template 1: [`fragments/layout.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/fragments/layout.html)
* **Role**: Master page layout fragment for all screens.
* **Fragments Defined**:
  1. `page-head(pageTitle)` — Includes character encoding, dynamic `<title>`, viewport meta tags, `<meta name="_csrf">` and `<meta name="_csrf_header">` tokens, `app.css`, and `app.js`.
  2. `top-bar` — Brand header displaying `${shopName}`, logged-in tailor badge (`${currentUser.fullName}`, `${currentUser.role}`), and a POST `/logout` button with CSRF token.
  3. `flash-messages` — Alert banner block rendering `${successMessage}`, `${errorMessage}`, `${loginError}`, and `${logoutMessage}`.
  4. `bottom-bar` — Sticky bottom navigation bar with active-tab highlighting (`Home`, `New Order`, `Customers`, `More`).

### Template 2: [`auth/login.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/auth/login.html)
* **Role**: Staff login form.
* **Fields**: 10-digit mobile number, password, CSRF hidden input, submit button. Includes link to `/signup` if allowed.

### Template 3: [`auth/signup.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/auth/signup.html)
* **Role**: First-run shop setup or authenticated owner staff creation screen.
* **Fields**: Shop Name (shown on first run), Full Name, Mobile Number, Password, Role selector.

### Template 4: [`home/index.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/home/index.html)
* **Role**: Main application landing dashboard.
* **Displays**: Welcome card, shop name, logged-in tailor's full name, and quick navigation cards.

### Template 5: Error Templates (`error/403.html`, `error/404.html`, `error/5xx.html`, `error/error.html`)
* **Role**: User-friendly error pages styled with shop branding, clear error descriptions, and return-home action links.

### Template 6: [`customers/index.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/customers/index.html)
* **Role**: Customer directory list and search screen.
* **Features**:
  * Clean search bar with input name `q`, autofocus, search button (`🔍 Search`), and conditional `Clear` button.
  * Active search indicator banner showing `"Showing search results for: Ramesh"`.
  * Results count badge (e.g. `"3 customer(s)"`).
  * Empty state with contextual messages when no customers match a search query vs when the directory is completely empty.
  * Desktop-first data table displaying row numbers, customer names (clickable to profile), phone numbers with monospace formatting, and a `"View Profile →"` action button.

### Template 7: [`customers/detail.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/customers/detail.html)
* **Role**: Customer profile detail view.
* **Features**:
  * Navigation breadcrumb link back to the customer directory (`← Back to Customer Directory`).
  * Profile header avatar and customer ID badge (`#1`).
  * Details grid showing:
    * Mobile number with clickable `tel:` link for direct phone dialing.
    * Total orders placed count (`<strong th:text="${orderCount}">`).
    * Last order date (or `"No orders yet"` if empty).
  * Quick action buttons: `"← Back to Customers"` and `"➕ Create New Order"`.

### Asset 1: [`static/css/app.css`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/static/css/app.css)
* **Role**: Desktop-first styling system.
* **Features**:
  * CSS custom properties (color palette: royal purple `#7b2cbf`, warm gold `#e0a96d`).
  * Top navigation bar, form cards, button components, and alert banners.
  * Customer search box styles (`.search-box`, `.search-input-group`, `.search-indicator`).
  * Data table styles (`.data-table`, `.table-header`, `.badge`, `.table-responsive`).
  * Customer profile styling (`.customer-profile-card`, `.profile-avatar`, `.profile-details-grid`, `.detail-label`, `.phone-link`).

### Asset 2: [`static/js/app.js`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/static/js/app.js)
* **Role**: Client-side JavaScript utilities.
* **Functions**:
  1. `getCsrfToken()` — Reads `<meta name="_csrf">` content string for AJAX requests.
  2. `getCsrfHeader()` — Reads `<meta name="_csrf_header">` content string.
  3. Auto-dismiss handler — Automatically fades out and removes `.alert` message banners after 5 seconds.
