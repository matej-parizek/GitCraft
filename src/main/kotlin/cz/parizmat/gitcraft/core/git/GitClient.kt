package cz.parizmat.gitcraft.core.git

import arrow.core.Either
import cz.parizmat.gitcraft.core.domain.base.GitError
import cz.parizmat.gitcraft.core.domain.base.ProcessError
import cz.parizmat.gitcraft.core.domain.element.Branch
import cz.parizmat.gitcraft.core.domain.element.Commit
import cz.parizmat.gitcraft.core.domain.element.GitChange
import cz.parizmat.gitcraft.core.domain.element.Repository
import java.nio.file.Path

interface GitClient {
    suspend fun openRepository(path: Path): Either<ProcessError, Repository>
    suspend fun status(repository: Repository): Either<GitError, List<GitChange>>
    suspend fun branches(repository: Repository): Either<GitError, List<Branch>>
    suspend fun commits(repository: Repository): Either<GitError, List<Commit>>
}