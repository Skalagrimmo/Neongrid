# Міркування щодо подальшого розвитку NeonGrid

## Коротке резюме

NeonGrid зараз виглядає як Android RPG-прототип на Kotlin/Jetpack Compose з сильним акцентом на кіберпанкову естетику, ізометричне поле, стелс, бойову систему, розвиток персонажа, екіпірування та локальне збереження прогресу через Room. Проєкт уже має кілька важливих переваг: розділення на UI, engine, render, model і data-пакети; наявність юніт- та Robolectric-тестів; перші системи для stealth/combat/progression; окремий native/isometric canvas напрям.

Моя головна рекомендація: не розширювати контент хаотично, а спершу стабілізувати ядро гри — ігровий цикл, модель стану, збереження, навігацію, тестованість і продуктивність рендера. Після цього можна масштабувати місії, ворогів, предмети та мета-прогрес без ризику, що кожна нова фіча буде ламати базову гру.

## Що вже є сильного

- **Виразна творча ідентичність.** UI, назви предметів, навичок і логів уже формують зрозумілий cyberpunk/stealth RPG тон.
- **Модульні системи gameplay.** Окремі `MovementSystem`, `CombatSystem`, `StealthAiSystem`, `LevelManager`, `SkillTreeManager` дають хорошу базу для тестування і розширення.
- **Compose-first інтерфейс.** Екрани меню, HUD, loadout, skill tree та canvas можна швидко ітеративно покращувати.
- **Persistence уже закладений.** Room-сутності для save state, inventory, stats, skills і loadouts показують, що проєкт рухається до повноцінної RPG-прогресії.
- **Тести не є формальністю.** Є перевірки AI state machine, data repository, skill tree UI/viewmodel і stealth overlay, тобто тестами покриті не лише приклади.

## Основні ризики, які варто закрити першими

### 1. Надто великий `GameViewModel`

`GameViewModel` зараз поєднує навігацію, ігровий цикл, стан гравця, ворогів, persistence, UI-флаги, логування, loadout-и, quest-прогрес і виклики систем. Це зручно для прототипу, але з часом ускладнить баланс, тестування і дебаг.

**Пропозиція:** поступово винести відповідальності у менші компоненти:

- `GameSessionController` — старт/стоп/тик гри, game over/win;
- `RunStateStore` — mutable стан поточного забігу;
- `ProgressionController` — XP, level-up, credits, skills;
- `SaveGameMapper` — перетворення runtime-моделі в Room-сутності й назад;
- `ScreenNavigator` або простий navigation state holder — переходи між екранами.

### 2. Дві паралельні лінії persistence

У коді є `GameRepository` навколо `GameDatabase`, а також ширший `DataRepository`, який працює з `AppDatabase`, `GameDatabase`, skills, inventory, effects і save state. Це може призвести до дублювання джерел істини.

**Пропозиція:** визначити одну canonical persistence-архітектуру:

- або один `AppDatabase` для всіх ігрових даних;
- або чітко розділити `GameDatabase` як save-slot базу, а `AppDatabase` як character/profile/meta базу;
- описати це у `README` або ADR і додати міграційні тести замість destructive migration для релізних даних.

### 3. Runtime-модель і Room-модель змішані концептуально

`Player`, `Enemy`, `EquipmentItem`, `SkillNode` — це runtime/gameplay-модель. Room-сутності — persistence-модель. Зараз між ними вже є серіалізація через рядки, наприклад comma-separated skill/equipment ids. Для MVP це нормально, але надалі зростатиме ризик невалідних id, втрати даних і складних міграцій.

**Пропозиція:** додати mapper-шар і типізовані value objects:

- `PlayerSaveStateMapper.toRuntime()` / `fromRuntime()`;
- typed wrappers для `SkillId`, `EquipmentId`, `QuestId` або хоча б centralized validators;
- JSON/Moshi serialization для складніших структур, якщо формат рядків почне розростатися.

### 4. Баланс гри поки закодований у Kotlin-об'єктах

Предмети, навички, вороги, шкода, XP, credits і енергетичні cost-и переважно hardcoded. Це пришвидшує старт, але ускладнює балансування.

**Пропозиція:** винести баланс у data-driven конфіги:

- JSON assets для equipment, enemy archetypes, skill tree, level objectives;
- невеликий validation test, який перевіряє унікальність id, існування prerequisites і діапазони числових параметрів;
- debug-екран або dev overlay для швидкої зміни damage/vision/speed без перекомпіляції.

## Рекомендована дорожня карта

### Етап 1 — стабілізація ядра

1. Додати `README.md` з описом гри, стеку, запуску, тестів і архітектури.
2. Зафіксувати architecture decision record для persistence: одна база чи дві, які сутності де живуть.
3. Продовжити розділення `GameViewModel`: save/load mapping, game-loop controller і screen-state controller уже винесені, наступний кандидат — progression.
4. Додати чисті JVM-тести для `CombatSystem`, `MovementSystem`, `GameStateSerializationService` і skill prerequisites.
5. Продовжити перевірку mutable state у Compose: enemies/noise/projectiles/logs уже варто тримати в observable collections, наступними лишаються складніші map/level mutations.

### Етап 2 — якість gameplay-loop

1. Визначити core loop однією фразою, наприклад: “проникнути на рівень, уникнути/нейтралізувати охорону, зламати цілі, евакуюватися, прокачати билд”.
2. Додати 3 типи місій: extraction, sabotage, rescue/data-heist.
3. Розширити AI: guard roles, patrol routes, investigation memory, alarm propagation.
4. Зробити stealth зрозумілішим для гравця: індикатори line-of-sight, sound radius, cover state, alert source.
5. Балансувати builds так, щоб ronin, necromancer і infiltrator мали різні способи проходження, а не лише різні modifiers.

### Етап 3 — контент і replayability

1. Винести предмети, навички й ворогів у data files.
2. Додати генератор секторів або хоча б набір handcrafted room templates.
3. Ввести модифікатори забігів: blackout, heavy patrols, toxic floor, camera network, elite hunter.
4. Додати meta-progression між run-ами: reputation, vendors, factions, permanent augment unlocks.
5. Додати codex/lore log, який відкривається через hacking і collectible fragments.

### Етап 4 — production hardening

1. Замінити destructive migration на явні Room migrations перед будь-яким релізом.
2. Додати CI: lint, unit tests, Robolectric tests, screenshot tests за потреби.
3. Налаштувати baseline profiles або performance benchmarks для canvas/render систем.
4. Перевірити cold start, memory allocations per frame, стабільність game loop при background/foreground.
5. Додати crash/error logging policy без витоку приватних даних.

## Технічні покращення з найбільшим ROI

- **State immutability для UI.** Замінити прямі mutable lists на `SnapshotStateList` або immutable copies у state holders, щоб Compose не пропускав оновлення.
- **Pure domain tests.** Максимально відокремити бойові формули, detection formulas і progression від Android APIs.
- **Save/load контракт.** Додати round-trip tests: runtime state -> save entity -> runtime state має повертати той самий прогрес.
- **Content validation.** Кожен skill prerequisite, equipment id, default loadout і quest objective повинен перевірятися тестом.
- **Performance budgets.** Встановити цілі: наприклад 30 FPS на low-spec mode і 60 FPS на full shader mode, з окремими вимірами для render/update.

## Продуктові ідеї

- **Build fantasy:** зробити три класи максимально різними: Ronin — ризикований ближній бій; Tech Necromancer — контроль камер/турелей/трупів-дронів; Ghost Infiltrator — route planning, vanish, silent extraction.
- **Readable stealth:** гравець має завжди розуміти, чому його побачили: світло, звук, дистанція, висота, кут огляду чи alarm network.
- **Сильні короткі місії:** мобільному формату пасують 5–10-хвилинні забіги з чіткою ціллю і meaningful reward.
- **NeonGrid як roguelite tactics:** якщо додати procedural sectors, модифікатори й meta-progression, проєкт може вирости з прототипу в replayable тактичну RPG.

## Найближчий практичний план на 2 тижні

1. Створити README і architecture note для поточного стану.
2. Покрити тестами serialization, combat hit/miss, level-up і skill unlock prerequisites.
3. Винести save/load mapping з `GameViewModel` в окремий клас.
4. Перевести enemies/noise/projectiles на state-friendly collections.
5. Додати один vertical slice місії: terminal objective + guarded route + extraction tile + reward screen.

## Висновок

Проєкт має сильну атмосферу і вже достатньо систем, щоб рухатися не просто як демо, а як основа для мобільної tactical stealth RPG. Найкраща стратегія розвитку — спершу укріпити архітектуру і тестованість, потім перевести баланс у data-driven формат, і лише після цього активно нарощувати контент. Це зменшить технічний борг і дозволить швидко експериментувати з місіями, білдами та ворогами без переписування ядра гри.
