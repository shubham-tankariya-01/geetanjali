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

  7. `public List<CustomerOrder> getCustomerOrders(Long customerId)`
     * **Signature**: Takes `customerId` (`Long`).
     * **In-Built Functions Used**:
       * `customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)`: Queries all past orders placed by this customer, sorted chronologically descending.
     * **Returns**: `List<CustomerOrder>` — Order history.
     * **Caller / Destination**: `CustomerController.showCustomerDetail()` for populating the customer profile order history table.

---

---

### Service 7: [`MeasurementService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/MeasurementService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/MeasurementService.java`
* **Real-Life Analogy**: The master tailor's measurement comparison ledger and snapshot engine.
* **Class Explanation**:
  * Marked with `@Service` and `@Transactional(readOnly = true)`.
  * Implements **Snapshot Immutability**: Tailoring measurements change over time, but historical orders must never change. When saving measurements, it checks if values match the latest recorded snapshot. If identical, it reuses the existing snapshot ID. If any value differs, it inserts a new `Measurement` record and associated `MeasurementValue` rows, preserving previous order history intact.
* **Functions & Methods**:
  1. `public Measurement saveSnapshot(Customer customer, Garment garment, Map<Long, BigDecimal> submittedValues)`
     * **Annotation**: `@Transactional` (write transaction).
     * **Signature**: Takes `customer` (`Customer`), `garment` (`Garment`), `submittedValues` (`Map<Long, BigDecimal>` of fieldId -> numeric value).
     * **Logic**: Compares submitted map against latest snapshot. If identical, reuses existing snapshot. If differing or absent, saves new `Measurement` and `MeasurementValue` rows.
     * **Returns**: `Measurement` snapshot entity.
     * **Caller / Destination**: `OrderService.placeOrder()` and `OrderService.updateOrderItem()`.
  2. `public boolean compare(Map<Long, BigDecimal> submitted, List<MeasurementValue> current)`
     * **Signature**: Takes `submitted` map and `current` measurement value list.
     * **Returns**: `true` if all numeric values match (`compareTo == 0`), `false` otherwise.
  3. `public Optional<Measurement> getLatestSnapshot(Long customerId, Long garmentId)`
     * **Signature**: Takes `customerId` and `garmentId`.
     * **Returns**: `Optional<Measurement>` most recent snapshot.
  4. `public Map<Long, BigDecimal> getLatestValues(Long customerId, Long garmentId)`
     * **Signature**: Takes `customerId` and `garmentId`.
     * **Returns**: `Map<Long, BigDecimal>` fieldId -> numeric value for auto-populating measurement forms.

---

### Service 8: [`OrderService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/OrderService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/OrderService.java`
* **Real-Life Analogy**: The tailor shop order book coordinator managing order booking, item customization, advance deposits, and item edits.
* **Class Explanation**:
  * Marked with `@Service` and `@Transactional(readOnly = true)`.
  * Orchestrates `CustomerOrderRepository`, `OrderItemRepository`, `ItemPhotoRepository`, `MeasurementService`, and `PaymentService`.
* **Functions & Methods**:
  1. `public CustomerOrder placeOrder(OrderDraft draft, AdvanceForm advanceForm, AppUser createdBy)`
     * **Annotation**: `@Transactional` (write transaction).
     * **Signature**: Takes session `draft` (`OrderDraft`), `advanceForm` (`AdvanceForm`), and `createdBy` (`AppUser`).
     * **Logic**: If `draft.getExistingOrderId()` is present, retrieves existing order, attaches new `OrderItem`s and `ItemPhoto`s, records additional advance deposit, and skips sequence generation. Otherwise, resolves customer, generates sequential `order_no` via `customerOrderRepository.getNextOrderNo()`, creates `CustomerOrder`, attaches garments/photos, and logs initial deposit.
     * **Returns**: `CustomerOrder` saved entity.
     * **Caller / Destination**: `OrderWizardController.handleSaveOrder()`.
  2. `public CustomerOrder getOrderOrThrow(Long id)`
     * **Signature**: Takes `id` (`Long`).
     * **Returns**: `CustomerOrder` or throws `ResourceNotFoundException`.
  3. `public OrderItem getOrderItemOrThrow(Long orderId, Long itemId)`
     * **Signature**: Takes `orderId` (`Long`) and `itemId` (`Long`).
     * **Returns**: `OrderItem` or throws `ResourceNotFoundException`.
  4. `public ItemForm prepareItemEditForm(OrderItem item)`
     * **Signature**: Takes `item` (`OrderItem`).
     * **Returns**: Prepopulated `ItemForm` with existing notes, price, delivery date, measurement values, and photos.
  5. `public OrderItem updateOrderItem(Long orderId, Long itemId, ItemForm form)`
     * **Annotation**: `@Transactional` (write transaction).
     * **Signature**: Takes `orderId`, `itemId`, and submitted `form` (`ItemForm`).
     * **Logic**: Verifies item belongs to order, saves/updates measurement snapshot via `measurementService.saveSnapshot()` (snapshot immutability), updates price, notes, deliveryDate, synchronizes `ItemPhoto` records, and saves item.
     * **Returns**: Updated `OrderItem`.
  6. `public List<CustomerOrder> listRecentOrders()`
     * **Signature**: None.
     * **Returns**: Top 20 recent orders via `customerOrderRepository.findTop20ByOrderByCreatedAtDesc()`.

---

### Service 9: [`OrderWizardService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/OrderWizardService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/OrderWizardService.java`
* **Real-Life Analogy**: The tailor's multi-step order booking clipboard held during customer fitting and consultation.
* **Class Explanation**:
  * Marked with `@Service`.
  * Encapsulates HTTP session state management for `OrderDraft` cart.
* **Functions & Methods**:
  1. `public OrderDraft getDraft()` — Retrieves existing session draft or initializes a fresh one.
  2. `public void startFresh()` — Resets session draft for a brand new order.
  3. `public void startAppendToOrder(CustomerOrder order)` — Clears draft, pre-populates customer info, and sets `existingOrderId` so wizard skips Step 1 and appends to this existing order.
  4. `public void setCustomer(Customer customer)` — Sets customer details on draft.
  5. `public void selectGarment(Garment garment)` — Sets current garment being measured.
  6. `public void addItem(ItemForm form)` — Converts `ItemForm` to `OrderDraftItem` and adds to cart.
  7. `public void updateItem(int index, ItemForm form)` — Updates draft item at index.
  8. `public void removeItemAndPhotos(int index, PhotoStorageService storage)` — Removes item from cart and deletes uploaded photos from disk.

---

### Service 10: [`PaymentService.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/service/PaymentService.java)
* **Location**: `src/main/java/com/geetanjali/backend/service/PaymentService.java`
* **Real-Life Analogy**: The cashier cash register recording payment receipts against orders.
* **Class Explanation**:
  * Marked with `@Service` and `@Transactional`.
* **Functions & Methods**:
  1. `public Payment recordPayment(CustomerOrder order, BigDecimal amount, PaymentMode mode)`
     * **Signature**: Takes `order` (`CustomerOrder`), `amount` (`BigDecimal`), `mode` (`PaymentMode`).
     * **Logic**: Validates amount > 0, resolves payment date via injected `Clock` (Asia/Kolkata), saves `Payment` entity.
     * **Returns**: `Payment` entity.

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
       * `customerService.getCustomerOrders(id)`: Fetches customer's full order history (newest first).
       * `measurementService.getLatestPerGarment(customer)`: Retrieves newest measurement snapshot per active garment.
     * **Model Injections**: `customer`, `orders`, `orderCount`, `lastOrderDate`, `garmentMeasurements`.
     * **Returns**: `String` view name `"customers/detail"`.
     * **Caller / Destination**: Browser loads `customers/detail.html`.

---

---

### Controller 7: [`OrderWizardController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/OrderWizardController.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/OrderWizardController.java`
* **Real-Life Analogy**: The multi-step booking desk attendant guiding tailors through customer selection, garment choice, measurements, and advance collection.
* **Class Explanation**:
  * Marked with `@Controller` and `@RequestMapping("/orders/new")`.
  * Manages the 4-step wizard workflow with session-scoped draft cart.
* **Functions & Methods**:
  1. `GET /orders/new` (`showCustomerStep`) — Entry point for order booking. If `customerId` query parameter is present, preselects customer in session draft and jumps immediately to Step 2 (`/orders/new/garment`). Otherwise resets draft to clean slate and displays Step 1 (`orders/wizard-customer`).
  2. `POST /orders/new/customer` (`handleCustomerStep`) — Validates/creates customer, updates draft, redirects to `/orders/new/garment`.
  3. `GET /orders/new/garment` (`showGarmentStep`) — Displays Step 2: Garment category cards.
  4. `POST /orders/new/garment` (`handleGarmentStep`) — Stores selected garment in draft, redirects to `/orders/new/item`.
  5. `GET /orders/new/item` (`showItemStep`) — Displays Step 3: Measurement entry form and photo dropzone. Auto-populates previous measurements if customer ordered this garment before.
  6. `POST /orders/new/item` (`handleSaveItem`) — Validates measurements, price, and delivery date. If action=`"more"`, redirects to `/orders/new/garment`; if action=`"finish"`, redirects to `/orders/new/summary`.
  7. `GET /orders/new/summary` (`showSummaryStep`) — Displays Step 4: Review cart items, order total, and advance payment form.
  8. `POST /orders/new/summary` (`handleSaveOrder`) — Atomically books order / appends items into PostgreSQL. Redirects to `/orders/{id}` for appends, or `/orders/new/success/{id}` for new orders.
  9. `GET /orders/new/success/{id}` (`showSuccessStep`) — Renders confirmation screen `orders/wizard-done.html`.
  10. `POST /orders/new/cancel` (`handleCancelWizard`) — Clears draft and cleans up photos.

---

### Controller 8: [`OrderController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/OrderController.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/OrderController.java`
* **Real-Life Analogy**: The order records manager handling order details, modifications, and adding items.
* **Class Explanation**:
  * Marked with `@Controller` and `@RequestMapping("/orders")`.
* **Functions & Methods**:
  1. `GET /orders/{orderId}` (`showOrderDetail`) — Loads order with items, payments, and photos. Computes balance due and delivery dates. Renders `orders/detail.html`.
  2. `GET /orders/{orderId}/add-items` (`startAddItemsToOrder`) — Calls `orderWizardService.startAppendToOrder(order)` and redirects directly to `/orders/new/garment` (skipping customer selection).
  3. `GET /orders/{orderId}/items/{itemId}/edit` (`showEditOrderItem`) — Loads existing measurements, price, notes, and photos into `ItemForm`. Renders `orders/item-edit.html`.
  4. `POST /orders/{orderId}/items/{itemId}` (`handleUpdateOrderItem`) — Validates form, calls `orderService.updateOrderItem()` (which preserves snapshot immutability), and redirects back to `/orders/{orderId}` with flash message.

---

### Controller 9: [`MoreController.java`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/java/com/geetanjali/backend/controller/MoreController.java)
* **Location**: `src/main/java/com/geetanjali/backend/controller/MoreController.java`
* **Real-Life Analogy**: The shop administration and settings office displaying owner configurations and shop roadmap extensions.
* **Class Explanation**:
  * Marked with `@Controller`. Handles `/more`, `/settings`, and `/garments`.
  * Integrates with `GlobalModelAdvice` to inspect `${isOwner}` and provide role-specific options.
* **Functions & Methods**:
  1. `public String showMorePage(Model model)`
     * **Annotation**: `@GetMapping("/more")`
     * **Logic**: Injects `garments` via `garmentService.listActiveGarments()`. Renders `"more"`.
  2. `public String showGarments(Model model)`
     * **Annotation**: `@GetMapping("/garments")`
     * **Logic**: Direct route loading active garments on the More hub.
  3. `public String redirectToMore()`
     * **Annotation**: `@GetMapping("/settings")`
     * **Logic**: Redirects legacy `/settings` requests to canonical `/more`.


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
* **Role**: Comprehensive customer profile view.
* **Features**:
  * Navigation breadcrumb link back to the customer directory (`← Back to Customer Directory`).
  * Profile header with customer avatar, name, ID badge (`#1`), and direct `tel:` mobile dialing link.
  * Quick action buttons: `"← Back to Customers"` and `"➕ New Order for this Customer"` (links to `/orders/new?customerId=...` which preselects customer and jumps straight to Step 2).
  * **Order History Table**: Chronological list of all orders placed by this customer (newest first). Each row displays clickable Order Number (e.g. `Order #1001`), item count, promised delivery date, total price, balance due status badge (`✓ Paid in Full` or `₹... Due`), and `"View Details →"` action button. Includes dedicated empty state when no orders exist yet.
  * **Saved Measurements Section**: Visual cards displaying the latest measurement snapshot recorded for each active garment type (Shirt, Pant, Kurta, Blouse) with snapshot ID badge, field names, numeric values, and units.

---

### Template 8: [`fragments/item-form.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/fragments/item-form.html)
* **Role**: Reusable garment customization form fragment shared by Wizard Step 3 (`orders/wizard-item.html`) and Order Item Edit (`orders/item-edit.html`).
* **Features**:
  * Dynamic measurement numeric input grid rendered based on active `garmentFields`.
  * Promised delivery date picker with `minDate` guard preventing past dates.
  * Stitching price input in rupees (`₹`).
  * Custom stitching notes textarea.
  * Multi-photo dropzone supporting camera capture and local preview.
  * Context-aware action buttons: `"Add Another Garment"` & `"Proceed to Summary"` in wizard mode vs `"Save Changes"` & `"Cancel"` in edit mode.

### Template 9: [`orders/wizard-customer.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/orders/wizard-customer.html)
* **Role**: Wizard Step 1: Customer identification.
* **Features**: Mobile number input with real-time AJAX lookup, customer name auto-fill, and existing customer selection cards.

### Template 10: [`orders/wizard-garment.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/orders/wizard-garment.html)
* **Role**: Wizard Step 2: Garment category selection.
* **Features**: Customer context summary bar, garment category cards (Shirt, Pant, Kurta, Blouse) with base pricing and visual icons.

### Template 11: [`orders/wizard-item.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/orders/wizard-item.html)
* **Role**: Wizard Step 3: Measurement entry and cloth photo attachments.
* **Features**: Embeds `fragments/item-form.html` with active draft summary counter and previous measurements auto-fill.

### Template 12: [`orders/wizard-summary.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/orders/wizard-summary.html)
* **Role**: Wizard Step 4: Multi-item review and advance payment collection.
* **Features**: Itemized cart cards with edit/delete buttons, financial summary (total amount, advance deposit, remaining balance due), and payment mode toggle (`CASH` / `UPI`).

### Template 13: [`orders/wizard-done.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/orders/wizard-done.html)
* **Role**: Order booking confirmation screen.
* **Features**: Large success badge, human-readable Order Number (e.g. `#1001`), customer info, balance due reminder, and action buttons (`"View Order"`, `"Book Another Order"`, `"Print Receipt"`).

### Template 14: [`orders/detail.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/orders/detail.html)
* **Role**: Complete order detail view.
* **Features**:
  * Order header with human-readable order number, link to customer profile, and creation timestamp.
  * Financial summary banner displaying Total Amount, Advance Paid, and Balance Due.
  * Garment items list with promised delivery date, price, stitching status badge, and `"✏️ Edit"` button.
  * Expandable `<details>` measurements snapshot drawer displaying all recorded numbers.
  * Cloth photos gallery with interactive lightbox modal pop-up (`imageLightboxModal`).
  * `"➕ Add Items to Order"` header button launching the wizard in append mode.

### Template 15: [`orders/item-edit.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/orders/item-edit.html)
* **Role**: Order item edit screen.
* **Features**:
  * Context banner indicating order number and garment type being modified.
  * Embeds `fragments/item-form.html` loaded with current item values.
  * Cancel button linking directly back to `/orders/{orderId}`.

---

### Template 16: [`more.html`](file:///home/shubham/storms/Projects/Geetanjali/backend/src/main/resources/templates/more.html)
* **Role**: Administration hub, settings, and upcoming extensions page.
* **Features**:
  * Header banner displaying current shop name, logged-in staff member full name, and role badge (`OWNER` / `TAILOR`).
  * **Owner-Only Administration Section** (`th:if="${isOwner}"`):
    * Garment catalog card showing active categories (Shirt, Pant, Kurta, Blouse) and link to `/garments`.
    * Staff management card linking to `/signup` to register new tailor staff members.
  * **Non-Owner Informational Notice** (`th:unless="${isOwner}"`): Explains owner-restricted configuration privileges to staff tailors.
  * **Roadmap & Extensions Grid**: Outlines upcoming features for Stage 2 (Stitching Workbench, Delivery Overrides) and Stage 3+ (Financial Analytics, WhatsApp & SMS alerts, Data Backup & CSV Export).
  * **Quick Navigation Shortcuts**: Direct buttons for Home Dashboard, Customers Directory, and New Order booking.


---

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
