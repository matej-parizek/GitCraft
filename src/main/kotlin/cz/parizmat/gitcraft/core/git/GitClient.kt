package cz.parizmat.gitcraft.core.git

import arrow.core.Either
import cz.parizmat.gitcraft.core.domain.base.GitError
import cz.parizmat.gitcraft.core.domain.base.ProcessError
import cz.parizmat.gitcraft.core.domain.element.Branch
import cz.parizmat.gitcraft.core.domain.element.Commit
import cz.parizmat.gitcraft.core.domain.element.GitChange
import cz.parizmat.gitcraft.core.domain.element.Repository
import cz.parizmat.gitcraft.core.domain.element.FileDiff
import java.nio.file.Path

interface GitClient {
    suspend fun openRepository(path: Path): Either<ProcessError, Repository>
    suspend fun status(repository: Repository): Either<GitError, List<GitChange>>
    suspend fun diff(repository: Repository, change: GitChange, staged: Boolean): Either<GitError, FileDiff>
    suspend fun stageFile(repository: Repository, change: GitChange): Either<GitError, Unit>
    suspend fun unstageFile(repository: Repository, change: GitChange): Either<GitError, Unit>
    suspend fun stageAll(repository: Repository): Either<GitError, Unit>
    suspend fun unstageAll(repository: Repository): Either<GitError, Unit>
    suspend fun discardChanges(repository: Repository): Either<GitError, Unit>
    suspend fun commit(repository: Repository, message: String, amend: Boolean = false): Either<GitError, Unit>
    suspend fun hasCommits(repository: Repository): Either<GitError, Boolean>
    suspend fun branches(repository: Repository): Either<GitError, List<Branch>>
    suspend fun commits(repository: Repository): Either<GitError, List<Commit>>
}
