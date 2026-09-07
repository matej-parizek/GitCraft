package cz.parizmat.gitcraft.core.domain.element.enums

enum class FileStatus {
    UNMODIFIED,
    MODIFIED,
    ADDED,
    DELETED,
    RENAMED,
    COPIED,
    UNTRACKED,
    IGNORED,
    CONFLICTED,
}