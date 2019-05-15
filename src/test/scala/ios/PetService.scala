package ios

import cats.effect._

final case class Pet(name: String)
final case class PetExistsError(name: String)

trait PetRepo {
  def save(pet: Pet): IO[Pet] = ???
  def findByName(name: String): IO[Option[Pet]] = ???
}

class PetService(repo: PetRepo) {
  def save(pet: Pet)(implicit cs: ContextShift[IO]): IO[Either[PetExistsError, Pet]] = {
    repo.findByName(pet.name).flatMap {
      case Some(_) => IO.pure(Left(PetExistsError(pet.name)))
      case None => repo.save(pet).map(p => Right(p))
    }
  }
}
