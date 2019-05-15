package futures

import scala.concurrent.{ExecutionContext, Future}

final case class Pet(name: String)
final case class PetExistsError(name: String)

// Oprah...
// YOU get an ExecutionContext, and YOU get an ExecutionContext, and YOU get an ExecutionContext
trait PetRepo {
  def save(pet: Pet)(implicit ec: ExecutionContext): Future[Pet] = ???
  def findByName(name: String)(implicit ec: ExecutionContext): Future[Option[Pet]] = ???
}

class PetService(repo: PetRepo) {
  def save(pet: Pet)(implicit ec: ExecutionContext): Future[Either[PetExistsError, Pet]] = {
    repo.findByName(pet.name).flatMap {
      case Some(_) => Future.successful(Left(PetExistsError(pet.name)))
      case None => repo.save(pet).map(p => Right(p))
    }
  }
}
