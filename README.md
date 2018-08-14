### Yet anothere test task

### Now in Scala

sbt inside, nothing special

- Set up the properties in application.conf in resources folder
- Run docker container for the outer service
`docker run -p 9000:9000 rockjam/elama-price-service:latest`
- Run as you'd like to (no judging!)
- :heart:


Comments on implementation:

- Circe json library was chosen for this project, basen on recomendation. Additionaly, Json macther was imported for easier testing. 
- Since jsons were relatively easy using "Optics" mechanism was chosen for accessing json fields and mapping them.
- The parts of the project were separated based on "responsibility zone": the properties, the user endpoint and the work with the outer service.

- Have I had more time, I would get more information about outer service, and describe the negative cases more precisely. Also maybe test integration more. As for the "real" project it may be reasonable to switch from Optics to case classes since the dynamic concept of this mechanism is non error-proof, though nice looking.
