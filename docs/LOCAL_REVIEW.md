# Lokalny przegląd OpenGym i uruchomienie

Przegląd wykonano 2026-10-14 dla źródeł
`https://github.com/DuarteSantos8/openGym`, commit
`e348fc17c42c7e9a211c5972e573ad3698190ee2` (wersja 1.4.1).

## Wynik bezpieczeństwa

W sprawdzonych fragmentach nie znaleziono dowodów na backdoor, koparkę ani
ukrytą wysyłkę danych. To przegląd źródeł i zależności, nie gwarancja braku
złośliwego kodu w całym łańcuchu dostaw.

Sprawdzono główny serwer, obsługę sesji, uprawnienia administratora, CSRF/CORS,
konfigurację i adaptery AI, wywołania procesów, ruch sieciowy frontendu,
Dockerfile/Compose oraz konfigurację i własne komponenty Android/iOS.
Pomocnicze wyszukiwanie podejrzanych konstrukcji objęło 377 plików kodu i
konfiguracji; nie wykryło dynamicznego eval, typowych wskaźników koparek,
prywatnych kluczy ani konstrukcji pobierz-skrypt-i-uruchom-powłokę w tym zakresie.
Nie objęło plików testowych, tłumaczeń, katalogu ćwiczeń ani dużych plików danych.
Oba lockfile pobierają paczki z registry.npmjs.org i deklarują integralność.
Nie przeprowadzono kompletnego audytu kodu każdej paczki, analizy wszystkich
binarnych artefaktów ani skanowania podatności systemowych obrazów kontenerów.

Zidentyfikowane i poprawione problemy:

1. `@capacitor/assets` wprowadzało siedem zgłoszeń podatności npm, w tym
   krytyczną w `tar` oraz wysokie w `sharp`/libvips. To narzędzia budowania ikon,
   nie dowód złośliwego kodu ani potwierdzenie zdalnej podatności serwera.
   Usunięto generator z domyślnych zależności. Istniejące ikony i splash screens
   pozostają w repozytorium. Regeneracja wymaga osobno sprawdzonego narzędzia.
   Po zmianie audyt całego frontendu: 0 znanych podatności.
   Audyt produkcyjnych zależności API bez opcjonalnych runtime AI: również 0.
2. Kontrola CSRF przyjmowała każdy nagłówek `Authorization: Bearer ...`, chociaż
   wybór sesji preferował ciasteczko. Test na oryginale potwierdził odpowiedź
   200 przy obcym Origin, ważnym ciasteczku i nieprawidłowym Bearer. Poprawka
   dopuszcza wyjątek tylko dla rzeczywiście uwierzytelnionej sesji Bearer.
   Nie potwierdzono wykorzystania w przeglądarce: polityka CORS nie zezwala
   na credentials i stanowi dodatkowe ograniczenie dla takiego scenariusza.
3. Healthcheck wget kierował lokalny test przez odziedziczone proxy, co
   dawało 403 mimo działającego API. Wyłączono proxy wyłącznie dla sprawdzania
   adresu 127.0.0.1; pobieranie z internetu nadal korzysta z normalnej konfiguracji.
4. Ograniczone uprawnienia plików checkoutu/COPY uniemożliwiały nginxowi
   odczyt szablonu i zasobów. Dockerfile ustawia odczyt plików oraz przechodzenie
   katalogów, pozostawiając nginx jako użytkownika 101.
5. Kontekst budowania wyklucza także pliki `.env.*` i katalog `coach-auth`.

Ruch sieciowy i funkcje wymagające świadomego włączenia:

- Zwykła synchronizacja korzysta z własnego serwera. `sendBeacon` służy temu
  serwerowi, a nie nieznanemu odbiorcy danych.
- Android sprawdza aktualizacje na GitHubie w ekranie informacji. Instalacja
  APK odbywa się po działaniu użytkownika; SHA-256 z tej samej publikacji
  sprawdza integralność, lecz nie jest niezależnym dowodem zaufania do wydawcy.
- Import Hevy, powiadomienia push i opcjonalny AI korzystają z zewnętrznych
  usług. AI może wysyłać dane treningowe do wybranego dostawcy oraz uruchamiać
  jego runtime. W dostarczonej konfiguracji `COACH_DISABLED=1` i target `default`
  wyłączają tę funkcję i nie instalują opcjonalnego SDK/CLI.
- Umami jest wstrzykiwane tylko przy ustawieniu obu zmiennych VITE_UMAMI_*;
  nie ustawiono ich w sprawdzanym buildzie.
- API pozostaje procesem root wewnątrz kontenera zgodnie z projektem upstream;
  lokalny Compose ogranicza capabilities, zabrania podnoszenia uprawnień i
  ustawia system plików API jako read-only z osobnym wolumenem danych.

## Wyniki kontroli

- Web build i mobile build: poprawne, ostrzeżenia o rozmiarze/dynamicznych
  importach nie zatrzymują kompilacji.
- Mobile: kopiowanie zasobów do Android/iOS i check-mobile-bundle przeszły.
- Nowy test CSRF oraz istniejące testy CORS i kompletności COPY: 6/6.
  Ten sam nowy test nie przechodzi na oryginale (200 zamiast 403).
- Pełny zestaw API uruchomiony podczas przeglądu: 586/596, 10 błędów
  dotyczących trenera AI. Dane przykładowe mają daty z lipca 2026; aktualne
  12-tygodniowe okno analizy już ich nie obejmuje. Odtworzono te błędy także
  na niezmienionym kodzie upstream. Poprawiono domyślne daty danych testowych
  i odpowiadające im oczekiwania, zachowując sprawdzanie całej struktury odpowiedzi.
  Pięć plików dotyczących tych błędów po zmianie: 85/85. Produkcyjne filtry
  ani reguła 12 tygodni nie zostały osłabione.
  Ponowiony pełny zestaw API po wszystkich poprawkach: **597/597**, 0 błędów.
- Pełny frontend: 5054/5060, sześć błędów uruchamiania subprocess w jednym
  pliku testów tłumaczeń w ograniczonym sandboxie. Ponowne uruchomienie całego
  tego pliku z dostępem sieciowym środowiska: 7/7. Nie powtarzano pozostałych
  401 plików, które przeszły w pierwszym uruchomieniu.
- Kontenery API/web: healthy. HTTP 200 dla strony, konfiguracji, API health na
  obu portach, wszystkich bezpośrednich zasobów JS/CSS strony, przykładowego
  obrazu ćwiczenia oraz POST /api/register/options z syntetycznym profilem.
  Nie przeprowadzono logowania prawdziwym sprzętowym passkey ani pełnego E2E UI.

Ważne ograniczenie weryfikacji Docker: pełny build oryginalnych Dockerfile
zatrzymała polityka sieci środowiska ChatGPT na dl-cdn.alpinelinux.org (403
przy apk upgrade). Zmiana opcji „Wyszukiwanie w sieci” nie odblokowała tego
hosta. Działanie sprawdzono w oddzielnych obrazach testowych: API z lokalnie
zainstalowanymi paczkami, web z lokalnie zbudowanym dist, na bazach Node/nginx.
Nie są to obrazy wydania i nie wykonano w nich etapu apk upgrade. Nie stanowi
to potwierdzenia pełnego builda dostarczonych Dockerfile ani aktualności ich
pakietów systemowych. Należy uruchomić pełny build na WSL według poniższej
instrukcji albo dopuścić host w konfiguracji środowiska i powtórzyć build tutaj.

## Własne repozytorium i WSL

Własna kopia znajduje się w `https://github.com/amik01/OpenGym`. Oryginał
pozostaje źródłem aktualizacji i historii autorstwa. Workflow CLA dotyczy
tylko repozytorium autora, a publikowanie obrazów i demo w tej kopii jest
domyślnie wyłączone. Można je później świadomie włączyć zmiennymi repozytorium
`OPENGYM_PUBLISH_IMAGES=true` oraz `OPENGYM_PUBLISH_DEMO=true`.
Testy CI pozostają aktywne. Nie podpisywano umowy CLA w imieniu właściciela.

Nie wymaga publikowania danych użytkowników ani katalogu `.git` w czacie.

```bash
git clone https://github.com/amik01/OpenGym.git ~/projekty/opengym
cd ~/projekty/opengym
git remote add upstream https://github.com/DuarteSantos8/openGym.git
```

Jeżeli katalog już istnieje, użyj go zamiast klonować ponownie. Sprawdź
`git status --short` i zachowaj swoje niezacommitowane zmiany przed zmianą gałęzi.
Po scaleniu zmian do main uruchom:

```bash
docker compose -f compose.local.yml config --quiet
docker compose -f compose.local.yml build --pull
docker compose -f compose.local.yml up -d --wait
docker compose -f compose.local.yml ps
curl -fsS http://localhost:8090/api/health
curl -fsS http://localhost:8091/api/health
```

Otwórz `http://localhost:8090`. Port 8091 udostępnia samo API diagnostyczne.
Nie potrzeba pliku `.env` ani lokalnego Node do uruchomienia tych kontenerów.
Oba porty są przypięte do 127.0.0.1; ten adres dotyczy komputera wykonującego
polecenia. Kontenery uruchomione w ChatGPT nie są kontenerami na Twoim Dellu.
Zatrzymanie: `docker compose -f compose.local.yml down`. Nie dodawaj `-v`,
jeżeli chcesz zachować dane. Dane zapisują się w osobnym wolumenie
`opengym-local_opengym-data`, niezależnym od ewentualnego starego `./data`.

Gdy build na WSL nie przejdzie, zbierz jego rzeczywisty komunikat:

```bash
docker compose --progress plain -f compose.local.yml build > build.log 2>&1
```

Po sprawdzeniu logu pod kątem haseł/tokenów załącz go w czacie. Nie zalecano
wyłączania TLS, pomijania błędów apk ani używania `npm audit fix --force`.

Do dalszej pracy twórz osobne gałęzie od aktualnego main:

```bash
git switch main
git pull --ff-only origin main
git switch -c feature/nazwa-zmiany
```

Kopia źródłowego repo i Twój Fork zachowują licencję AGPL oraz ograniczenia
licencji materiałów ćwiczeń opisane w NOTICE.md.

## iOS

Projekt jest już w `frontend/ios`, oparty na Capacitor 7. Minimalny target
aplikacji to iOS 15.5. Potrzebny jest Mac, Xcode 16+ dla Capacitor 7,
Node 22.12+ (lub wspierany nowszy Node) oraz CocoaPods. Na Macu:

```bash
cd frontend
npm ci --ignore-scripts
npm run build:mobile
node_modules/.bin/cap open ios
```

`build:mobile` buduje wersję mobilną, dołącza media, wykonuje cap sync i
sprawdza pakiet. Otwieraj workspace wygenerowany przez Capacitor/CocoaPods,
nie sam plik projektu bez Pods. W Xcode wybierz Signing & Capabilities,
własny Team i unikalny Bundle Identifier, a następnie symulator lub własny
iPhone i Run. Dla własnego urządzenia możliwy jest darmowy Apple ID z
ograniczeniami podpisu, w tym zwykle koniecznością odnowienia po 7 dniach.
App Store/TestFlight wymagają odpowiedniej konfiguracji i członkostwa Apple.

Do testu samego interfejsu na symulatorze użyj trybu lokalnego. Do połączenia
z serwerem na Dellu potrzebny jest adres osiągalny z Maca/iPhone'a: localhost
na telefonie oznacza telefon. Obecny Compose celowo nie wystawia portów do LAN.
Najlepiej udostępnić web przez reverse proxy HTTPS z zaufanym certyfikatem i
ustawić ORIGIN oraz RP_ID na właściwy adres. Nie wyłączaj globalnie ATS/TLS
w aplikacji iOS. Rozwiązanie dostępu sieciowego zależy od Twojej sieci/domeny.

Natywnej kompilacji, podpisania ani uruchomienia w Xcode nie wykonano w
środowisku Linux. Zweryfikowano build frontendowy oraz skopiowany pakiet iOS;
pozostają instalacja Pods, kompilacja Swift i odbiór na urządzeniu/symulatorze.
Właściciel potwierdził brak Maca; obecny zakres obejmuje przygotowanie projektu
i instrukcji, bez deklarowania wykonanej kompilacji natywnej.
