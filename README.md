Домашнее задание: CI/CD, TeamCity Голоха Е.В.
Автор: Евгений Голоха
Репозиторий: https://github.com/zis-git/example-teamcity
CI-сервер: TeamCity (server: 158.160.205.28:8111)
Агент: ip_158.160.184.84
Артефакты: Nexus OSS (158.160.130.77:8081)

--------------------------------------------------
1. Краткое описание задания
--------------------------------------------------

1. Настроить сборку Java-проекта в TeamCity c использованием Maven.
2. Вынести конфигурацию TeamCity в репозиторий (Versioned Settings, Kotlin DSL).
3. Настроить сборку в отдельной ветке feature/add_reply: добавить новый метод hunter и тесты.
4. Настроить разное поведение для веток:
   - для master — выполнять deploy в Nexus;
   - для любых других веток — только тесты.
5. Убедиться, что настройки TeamCity хранятся в репозитории в каталоге .teamcity.
6. Выполнить сборки, предоставить вывод и скриншоты.

--------------------------------------------------
2. Подготовка окружения
--------------------------------------------------

1. Развёрнут сервер TeamCity в Яндекс.Облаке по адресу 158.160.205.28:8111.
2. Развёрнут Build Agent (Linux) 158.160.184.84 и успешно подключён к серверу TeamCity.
3. Развёрнут Nexus OSS 3.14.0-04 по адресу 158.160.130.77:8081, созданы репозитории:
   - maven-releases
   - maven-snapshots
   - maven-public (групповой репозиторий)
4. Склонирован учебный репозиторий example-teamcity в GitHub:
   - origin: git@github.com:zis-git/example-teamcity.git
   - локальный каталог на агенте: /home/zis/example-teamcity
<img width="2159" height="1373" alt="2025-12-05_00-26-45" src="https://github.com/user-attachments/assets/739ef55a-87ee-4c1b-9731-446482237b50" />

--------------------------------------------------
3. Настройка VCS Root в TeamCity
--------------------------------------------------

1. В проекте netology создан VCS Root типа Git.
2. Основные параметры (см. Скриншот 4):
   - Fetch URL: https://github.com/zis-git/example-teamcity.git
   - Default branch: refs/heads/master
   - Branch specification: refs/heads/*
   - Authentication method: Password / personal access token
   - Username: zis-git
   - Токен GitHub с правами Contents: Read and write.
3. Нажат «Test Connection» — соединение успешно.
4. VCS Root привязан к build configuration Build.

<img width="2159" height="1373" alt="2025-12-05_00-26-45" src="https://github.com/user-attachments/assets/503aed00-ec11-4804-9892-8b3390bb6473" />
 — Настройки VCS Root в TeamCity для репозитория example-teamcity.

--------------------------------------------------
4. Включение Versioned Settings (Kotlin DSL)
--------------------------------------------------

1. В проекте netology включён режим:
   - Synchronization enabled
   - Project settings VCS root: тот же Git-репозиторий (ветка master).
   - Settings format: Kotlin.
   - Settings path in VCS: .teamcity
2. После включения синхронизации TeamCity сделал первый коммит с директориями:
   - .teamcity/settings.kts
   - .teamcity/pom.xml
   - .teamcity/pluginData/_Self/...
3. На агенте выполнено:
   - git pull
   - ls -la
   В корне репозитория появилась директория .teamcity.

<img width="2159" height="1373" alt="2025-12-05_00-26-45" src="https://github.com/user-attachments/assets/9b2f2b85-2c70-4251-91d3-845990edb72b" />
 — Файл .teamcity/settings.kts в GitHub (ветка master), виден Kotlin DSL и объект Build.

--------------------------------------------------
5. Изменение кода: новая ветка feature/add_reply
--------------------------------------------------

1. На агенте создана ветка:
   - git checkout -b feature/add_reply
2. В файле src/main/java/plaindoll/Welcomer.java добавлен новый метод:

   public String hunter(String name) {
       return "Hunting for " + name + "!";
   }

3. В тестах src/test/java/plaindoll/WelcomerTest.java добавлен тест:

   @Test
   public void welcomerHunterMethod() {
       assertThat(welcomer.hunter("Hunter"), containsString("Hunting for Hunter!"));
   }

4. Выполнен коммит и пуш:
   - git add src/main/java/plaindoll/Welcomer.java src/test/java/plaindoll/WelcomerTest.java
   - git commit -m "Add hunter method and test"
   - git push --set-upstream origin feature/add_reply

<img width="2001" height="836" alt="2025-12-05_00-20-11" src="https://github.com/user-attachments/assets/4d09f266-ee42-48a5-bbd8-a377682f1690" />
 — История коммитов в ветке master в GitHub, видны коммиты:
   - Add hunter method and test
   - Add VCS trigger to build all branches
   - Move VCS trigger into Build buildType
   - Versioned settings configuration updated (TeamCity change in 'netology' project).

--------------------------------------------------
6. Настройка Kotlin DSL (.teamcity/settings.kts)
--------------------------------------------------

Файл .teamcity/settings.kts в ветке master (фрагмент):

- Определение проекта и конфигураций:

   version = "2025.11"

   project {
       buildType(Build)
       buildType(HttpsGithubComZisGitExampleTeamcityGit)
   }

- Основная конфигурация Build:

   object Build : BuildType({
       name = "Build"

       artifactRules = "target/*.jar => artifacts"

       vcs {
           root(DslContext.settingsRoot)
       }

       steps {
           // Шаг для всех веток, кроме master
           maven {
               id = "Maven2"
               conditions {
                   doesNotEqual("teamcity.build.branch", "master")
               }
               goals = "clean test"
               runnerArgs = "-Dmaven.test.failure.ignore=true"
           }

           // Шаг deploy только для master
           maven {
               id = "deployMaster"
               conditions {
                   equals("teamcity.build.branch", "master")
               }
               goals = "clean deploy"
               userSettingsSelection = "settings.xml"
           }
       }

       // VCS-триггер на любые ветки
       triggers {
           jetbrains.buildServer.configs.kotlin.triggers.vcs {
               branchFilter = "+:*"
           }
       }
   })

Таким образом, для ветки master выполняется полный цикл clean deploy,
для всех остальных веток — только clean test.

<img width="2139" height="831" alt="2025-12-05_00-18-54" src="https://github.com/user-attachments/assets/33b5befd-25df-4997-a27f-2287640e6375" />
<img width="2257" height="848" alt="2025-12-05_00-17-58" src="https://github.com/user-attachments/assets/4aefff76-9729-41d6-8fa0-e92b029d49c5" />
<img width="2093" height="1021" alt="2025-12-05_00-29-13" src="https://github.com/user-attachments/assets/9005f1b5-ecf8-48ba-8e66-8b95a1fcfe21" />

 — Полное содержимое .teamcity/settings.kts в GitHub с блоком triggers { vcs { branchFilter = "+:*" } }.

--------------------------------------------------
7. Слияние feature/add_reply в master
--------------------------------------------------

1. После проверки сборки в ветке feature/add_reply выполнено слияние в master:

   git checkout master
   git merge feature/add_reply
   git push

2. В истории master в GitHub появился merge-коммит с изменениями:
   - новый метод hunter и тесты;
   - обновлённый settings.kts с триггером.

<img width="2189" height="1294" alt="2025-12-05_00-24-30" src="https://github.com/user-attachments/assets/62f3a0a6-e451-4b4a-95d1-f2d6bf1e9efb" />
 — История коммитов master в GitHub с указанными коммитами и TeamCity-изменениями.

--------------------------------------------------
8. Сборка проекта в разных ветках
--------------------------------------------------

8.1. Сборка master

1. В TeamCity запущена сборка Build #5 на ветке master.
2. Шаги Maven:
   - Step 1: clean test — успешно, Tests passed: 5
   - Step 2: clean deploy — попытка деплоя в Nexus (maven-releases)
3. Деплой завершился ошибкой 400:

   Repository does not allow updating assets: maven-releases (400)

   Это ожидаемое поведение: версия 0.0.3 уже существует в репозитории
   maven-releases, и перезапись релизных артефактов запрещена политикой Nexus.

4. Итог:
   - Tests passed: 5
   - Build завершён со статусом exit code 1 на шаге deploy.

<img width="2159" height="1373" alt="2025-12-05_00-26-45" src="https://github.com/user-attachments/assets/b9e304a6-de95-45b6-a4f7-7ffd55c02ee3" />
 — Build #5 (master) в TeamCity: зелёные тесты, ошибка на шаге Maven deploy.

8.2. Сборка feature/add_reply

1. Ветке feature/add_reply соответствует сборка Build #7.
2. Конфигурация Build благодаря условиям в settings.kts выполняет только шаг:

   - Maven2: goals "clean test"

3. Результат:
   - Tests passed: 6 (учитывая новый тест hunter)
   - Deploy не выполняется (условие equals("teamcity.build.branch", "master") ложно)
   - Сборка завершается успешно.

<img width="1239" height="1255" alt="2025-12-05_00-33-51" src="https://github.com/user-attachments/assets/a9644ed2-4da8-4503-b2a8-ec615e5abdc2" />
 — Build #7 (feature/add_reply) в TeamCity: Tests passed: 6, статус Success.

8.3. Обзор по веткам

На вкладке Overview → Branches видна следующая картина:

- master:
  - несколько сборок, часть успешных, часть с ошибкой на deploy;
- feature/add_reply:
  - Build #7 — успешная, Tests passed: 6.

<img width="2257" height="848" alt="2025-12-05_00-17-58" src="https://github.com/user-attachments/assets/73c00088-40b8-47c7-9750-b4ffb55b6f91" />
 — Обзор веток master и feature/add_reply с их сборками и статусами.

--------------------------------------------------
9. Nexus: проверка загрузки артефактов
--------------------------------------------------

1. В веб-интерфейсе Nexus (Browse → maven-public → org/netology/plaindoll)
   видно, что в репозиторий уже опубликованы версии:
   - 0.0.2
   - 0.0.3
2. Это объясняет ошибку "Repository does not allow updating assets" при попытке повторного deploy той же версии 0.0.3 из master.

<img width="2093" height="1021" alt="2025-12-05_00-29-13" src="https://github.com/user-attachments/assets/a77aa1b9-f5db-46bf-8316-99188e8a1e30" />
 — Nexus Repository Manager, артефакты org/netology/plaindoll/0.0.3.

--------------------------------------------------
10. Ответы на контрольные вопросы и вывод
--------------------------------------------------

1. Где хранятся настройки TeamCity?
   - Настройки проекта netology и билд-конфигурации Build
     хранятся в репозитории GitHub в каталоге .teamcity в формате Kotlin DSL.
     Включена синхронизация Versioned Settings.

2. Как реализовано различное поведение для веток master и feature/*?
   - В файле settings.kts в BuildType добавлены два Maven-шага с условиями:
     * doesNotEqual("teamcity.build.branch", "master") — clean test для всех веток, кроме master;
     * equals("teamcity.build.branch", "master") — clean deploy для master.
   - Дополнительно добавлен VCS-trigger с branchFilter = "+:*", чтобы
     сборки запускались при изменениях во всех ветках.

3. Почему сборка master завершается с ошибкой, а feature/add_reply — успешно?
   - В master выполняется deploy в репозиторий maven-releases.
     Версия 0.0.3 уже загружена, и Nexus запрещает перезаписывать релизные артефакты,
     поэтому deploy падает с кодом 400.
   - В feature/add_reply deploy не выполняется (условие по ветке), производится
     только clean test, все тесты проходят, поэтому сборка успешна.

4. Достигнут ли результат по заданию?
   - Да. Проект example-teamcity собран в TeamCity, настройки вынесены в Git,
     реализовано разное поведение для веток master и feature/add_reply,
     добавлен новый метод hunter и тесты, сборка feature/add_reply проходит
     успешно, а master выполняет deploy в Nexus и падает на повторной загрузке
     той же версии, что соответствует ожидаемому сценарию.

ИТОГОВЫЙ ВЫВОД:
Цель домашнего задания выполнена. Настроен полный цикл:
GitHub → TeamCity (Kotlin DSL, VCS triggers, Maven) → Nexus,
а также реализована работа с feature-веткой и хранением конфигурации
TeamCity в репозитории.
