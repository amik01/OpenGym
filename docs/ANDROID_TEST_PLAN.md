# Android przed migracją do Fluttera

Priorytet właściciela: najpierw ocena obecnego Androida; iOS odłożone.
Migracja do Fluttera wymaga osobnej decyzji po tej ocenie. Nie dodano Fluttera
ani nie zmieniono technologii aplikacji w tym etapie.

Właściciel nie ma jeszcze telefonu ani emulatora. Workflow
`Android APK and emulator` w GitHub Actions buduje testową APK ze źródeł
naszego repozytorium, wykonuje natywne testy jednostkowe i uruchamia Androida
15/API 35 na emulatorze x86_64. Nie wymaga Maca ani klucza podpisującego wydanie.
Weryfikuje start prawdziwego WebView/Capacitor, wybór trybu lokalnego przez UI,
zapis wyboru do prywatnego pliku Androida, lokalne zasoby ćwiczeń, przejście
do biblioteki i odtworzenie aktywności bez ponownego onboardingu. Zapisuje
APK i SHA-256 jako artefakt `OpenGym-Android-test`, a zrzut ekranu oraz raporty
oddzielnie jako `OpenGym-Android-reports` na 7 dni. APK budowana jest przed
startem emulatora. Konfiguracja ogranicza partycję danych do 2 GiB, lecz obraz
Google może wymagać większego pliku startowego; na jednorazowym runnerze
zwalniane są nieużywane SDK .NET/Haskell. Nie usuwa to niczego z komputera
właściciela. Przed testem wyłączane są Wi-Fi i dane komórkowe emulatora;
obraz ćwiczenia musi być rzeczywiście zdekodowany przez WebView.
Nie zastępuje to odbioru użytkowego wszystkich funkcji.

Debug ma osobny identyfikator `ch.duartesantos.opengym.test` i nazwę
`OpenGymTest`, więc nie nadpisuje zwykłej aplikacji. To testowy podpis, nie
wydanie do sklepu. Wrapper Gradle sprawdzany jest przed wykonaniem z oficjalnym
SHA-256; archiwum Gradle ma również ustawioną sumę dystrybucji.

## Odbiór użytkowy

Po zainstalowaniu Android Studio na Windows utwórz emulator telefonu z
Androidem 15 (API 35). Uruchom go i przeciągnij rozpakowane `app-debug.apk`
na okno emulatora. APK i raporty pobierzesz z zakończonego uruchomienia
workflow w zakładce Actions naszego repozytorium.

Do pierwszego testu wybierz „Use on this device”/tryb lokalny. Nie podawaj
rzeczywistych danych ani kont dostawców AI. Sprawdź i zanotuj wynik:

1. Start bez białego ekranu; czytelne przyciski i paski systemowe.
2. Biblioteka: wyszukiwanie, filtry, obraz i animacja ćwiczenia.
3. Plan: dodanie rutyny i ćwiczenia, zapis, ponowne otwarcie.
4. Trening: seria, ciężar/powtórzenia, timer odpoczynku, zakończenie, historia.
5. Edycja zakończonego treningu i poprawne statystyki.
6. Waga ciała, wykres i eksport/import wyłącznie danych przykładowych.
7. Zatrzymanie i ponowne uruchomienie aplikacji oraz tryb samolotowy:
   dane i biblioteka nadal dostępne.
8. Zmiana motywu/języka, systemowy Wstecz, klawiatura i obrót ekranu.
9. Powiadomienia i timer przy wygaszonym ekranie. Rzeczywisty telefon będzie
   potrzebny do rzetelnej oceny dźwięku/wibracji i oszczędzania baterii.
10. Dopiero po skonfigurowaniu adresu osiągalnego z emulatora/telefonu:
    parowanie, synchronizacja dwóch klientów, utrata sieci i ponowne połączenie.
    localhost w emulatorze oznacza emulator; adres hosta Android Studio to
    zwykle 10.0.2.2. ORIGIN musi odpowiadać adresowi web używanemu w przeglądarce.

Kod QR/kamera, Health Connect, zachowanie na prawdziwych urządzeniach oraz
pełna synchronizacja nie są potwierdzone samym testem uruchomienia emulatora.
Bez wyniku tych testów nie deklarujemy, że cała aplikacja działa dobrze.

## Warunek rozpoczęcia Fluttera

Najpierw udokumentujemy działające scenariusze i znalezione błędy. Następnie
ustalimy, które funkcje zachować i jak przenieść dane oraz testy reguł treningu.
Istniejące API i kontenery mogą pozostać backendem; zmiana klienta na Fluttera
nie wymaga automatycznie przepisywania serwera. Migracji nie rozpoczęto.
