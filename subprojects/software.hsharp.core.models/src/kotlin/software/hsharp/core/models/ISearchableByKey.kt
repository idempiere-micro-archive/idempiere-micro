package software.hsharp.core.models

interface ISearchableByKey {
    /**
     * Get the key you can use to search for the entity directly.
     * The key needs to be matched fully to return one single entity when used in search or instant search.
     * Since it is a key, it needs to be unique.
     * @return the search key.
     */
    val value: String
}