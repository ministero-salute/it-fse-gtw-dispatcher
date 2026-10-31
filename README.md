
# Fascicolo Sanitario 2.0

# _it-fse-gtw-dispatcher_


Il microservizio in oggetto ha la responsabilità di esporre al Consumer le API Rest per l'invio, la cancellazione, la sostituzione e l'aggiornamento metadati di un documento clinico.

<br/>

## Repos
- [*it-fse-support*](https://github.com/ministero-salute/it-fse-support) - Pagina di supporto FSE 2.0
- [*it-fse-landing*](https://github.com/ministero-salute/it-fse-landing) - Landing page dei repository FSE 2.0

<br/>

## Test isolati

Il profilo opt-in `ISOLATION` esegue i test JUnit contrassegnati `@Tag("ISOLATION")`,
senza avviare l'applicazione Spring Boot o richiedere MongoDB, Kafka, INI, EDS o altri
servizi esterni. Le dipendenze esterne sono simulate; parsing di documenti/PDF,
controlli JWT, mapping, richieste HTTP e workflow applicativi sono esercitati realmente.
I test ordinari restano separati dal profilo.

```sh
MAVEN_OPTS=-Xmx768m mvn -PISOLATION clean test
```

I report JaCoCo sono disponibili in `target/site/jacoco/`.

Per controllare anche la copertura combinata di righe e condizioni, dopo una nuova
esecuzione pulita dei test, usare il controllo locale (richiede Python 3):

```sh
python3 tools/check-isolation-coverage.py
```

I test del controllo locale si eseguono con
`python3 -m unittest discover -s tools -p 'test_*.py'`.

Il controllo legge il report XML e le esclusioni Sonar già dichiarate nel POM;
termina con codice non zero se la soglia non è rispettata. Non modifica la
selezione dei test o il comportamento della build ordinaria. L'analisi Sonar
resta il riferimento per le misure pubblicate dal server.

## Sonar

È disponibile `org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970`.
Il plugin OWASP dependency-check non è più dichiarato nel POM.

```sh
export SONAR_HOST_URL=https://sonar.example.org
export SONAR_TOKEN=...
MAVEN_OPTS=-Xmx768m mvn -PISOLATION clean test sonar:sonar -Dsonar.projectKey=gtw-dispatcher-ms
```

La pubblicazione richiede un server Sonar e credenziali valide; il comando di test
isolato non li richiede. Il report XML JaCoCo usa il percorso Maven standard.

## License

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
