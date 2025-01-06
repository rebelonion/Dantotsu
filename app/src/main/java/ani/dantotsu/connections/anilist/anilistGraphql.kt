package ani.dantotsu.connections.anilist

val standardPageInformation = """
  pageInfo {
    total
    perPage
    currentPage
    lastPage
    hasNextPage
  }
""".prepare()

fun String.prepare() = this.trimIndent().replace("\n", " ").replace("""  """, "")

fun characterInformation(includeMediaInfo: Boolean) = """
    id
    name {
      first
      middle
      last
      full
      native
      userPreferred
    }
    image {
      large
      medium
    }
    age
    gender
    description
    dateOfBirth {
      year
      month
      day
    }
    ${
    if (includeMediaInfo) """
    media(page: 0,sort:[POPULARITY_DESC,SCORE_DESC]) {
      $standardPageInformation
      edges {
        id
        voiceActors {
          id,
          name {
            userPreferred
          }
          languageV2,
          image {
            medium,
            large
          }
        }
        characterRole
        node {
          id
          idMal
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          isFavourite
          format
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              private
              score(format: POINT_100)
              status
          }
        }
      }
    }""".prepare() else ""
}
""".prepare()

fun studioInformation(page: Int, perPage: Int) = """
    id
    name
    isFavourite
    favourites
    media(page: $page, sort:START_DATE_DESC, perPage: $perPage) {
      $standardPageInformation
      edges {
        id
        node {
          id
          idMal
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          startDate{ year }
          isFavourite
          format
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              private
              score(format: POINT_100)
              status
          }
        }
      }
    }
""".prepare()

fun staffInformation(page: Int, perPage: Int) = """
    id
    name {
      first
      middle
      last
      full
      native
      userPreferred
    }
    image {
      large
      medium
    }
    dateOfBirth {
      year
      month
      day
    }
    dateOfDeath {
      year
      month
      day
    }
    age
    yearsActive
    homeTown
    staffMedia(page: $page,sort:START_DATE_DESC, perPage: $perPage) {
      $standardPageInformation
      edges {
        staffRole
        id
        node {
          id
          idMal
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          startDate{ year }
          isFavourite
          format
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              private
              score(format: POINT_100)
              status
          }
        }
      }
    }
""".prepare()

fun userInformation() = """
    id
    name
    about(asHtml: true)
    avatar {
      large
      medium
    }
    bannerImage
    isFollowing
    isFollower
    isBlocked
    siteUrl
""".prepare()

fun aniMangaSearch(perPage: Int?) = """
    query (${"$"}page: Int = 1, ${"$"}id: Int, ${"$"}type: MediaType, ${"$"}isAdult: Boolean = false, ${"$"}search: String, ${"$"}format: [MediaFormat], ${"$"}status: MediaStatus, ${"$"}countryOfOrigin: CountryCode, ${"$"}source: MediaSource, ${"$"}season: MediaSeason, ${"$"}seasonYear: Int, ${"$"}year: String, ${"$"}onList: Boolean, ${"$"}yearLesser: FuzzyDateInt, ${"$"}yearGreater: FuzzyDateInt, ${"$"}episodeLesser: Int, ${"$"}episodeGreater: Int, ${"$"}durationLesser: Int, ${"$"}durationGreater: Int, ${"$"}chapterLesser: Int, ${"$"}chapterGreater: Int, ${"$"}volumeLesser: Int, ${"$"}volumeGreater: Int, ${"$"}licensedBy: [String], ${"$"}isLicensed: Boolean, ${"$"}genres: [String], ${"$"}excludedGenres: [String], ${"$"}tags: [String], ${"$"}excludedTags: [String], ${"$"}minimumTagRank: Int, ${"$"}sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC, START_DATE_DESC]) {
      Page(page: ${"$"}page, perPage: ${perPage ?: 50}) {
        $standardPageInformation
        media(id: ${"$"}id, type: ${"$"}type, season: ${"$"}season, format_in: ${"$"}format, status: ${"$"}status, countryOfOrigin: ${"$"}countryOfOrigin, source: ${"$"}source, search: ${"$"}search, onList: ${"$"}onList, seasonYear: ${"$"}seasonYear, startDate_like: ${"$"}year, startDate_lesser: ${"$"}yearLesser, startDate_greater: ${"$"}yearGreater, episodes_lesser: ${"$"}episodeLesser, episodes_greater: ${"$"}episodeGreater, duration_lesser: ${"$"}durationLesser, duration_greater: ${"$"}durationGreater, chapters_lesser: ${"$"}chapterLesser, chapters_greater: ${"$"}chapterGreater, volumes_lesser: ${"$"}volumeLesser, volumes_greater: ${"$"}volumeGreater, licensedBy_in: ${"$"}licensedBy, isLicensed: ${"$"}isLicensed, genre_in: ${"$"}genres, genre_not_in: ${"$"}excludedGenres, tag_in: ${"$"}tags, tag_not_in: ${"$"}excludedTags, minimumTagRank: ${"$"}minimumTagRank, sort: ${"$"}sort, isAdult: ${"$"}isAdult) {
          ${standardMediaInformation()}
        }
      }
    }
""".prepare()

fun standardMediaInformation() = """
id
idMal
siteUrl
isAdult
status(version: 2)
chapters
episodes
nextAiringEpisode {
  episode
  airingAt
}
type
genres
meanScore
popularity
favourites
isFavourite
format
bannerImage
countryOfOrigin
coverImage {
  large
  extraLarge
}
title {
  english
  romaji
  userPreferred
}
mediaListEntry {
  progress
  private
  score(format: POINT_100)
  status
}
""".prepare()

fun fullMediaInformation(id: Int) = """
{
  Media(id: $id) {
    streamingEpisodes {
      title
      thumbnail
      url
      site
    }
    mediaListEntry {
      id
      status
      score(format: POINT_100)
      progress
      private
      notes
      repeat
      customLists
      updatedAt
      startedAt {
        year
        month
        day
      }
      completedAt {
        year
        month
        day
      }
    }
    reviews(perPage: 3, sort: SCORE_DESC) {
      nodes {
        id
        mediaId
        mediaType
        summary
        body(asHtml: true)
        rating
        ratingAmount
        userRating
        score
        private
        siteUrl
        createdAt
        updatedAt
        user {
          id
          name
          bannerImage
          avatar {
            medium
            large
          }
        }
      }
    }
    ${standardMediaInformation()}
    source
    duration
    season
    seasonYear
    startDate {
      year
      month
      day
    }
    endDate {
      year
      month
      day
    }
    studios(isMain: true) {
      nodes {
        id
        name
        siteUrl
      }
    }
    description
    trailer {
      site
      id
    }
    synonyms
    tags {
      name
      rank
      isMediaSpoiler
    }
    characters(sort: [ROLE, FAVOURITES_DESC], perPage: 25, page: 1) {
      edges {
        role
        voiceActors {
          id
          name {
            first
            middle
            last
            full
            native
            userPreferred
          }
          image {
            large
            medium
          }
          languageV2
        }
        node {
          id
          image {
            medium
          }
          name {
            userPreferred
          }
          isFavourite
        }
      }
    }
    relations {
      edges {
        relationType(version: 2)
        node {
          ${standardMediaInformation()}
        }
      }
    }
    staffPreview: staff(perPage: 8, sort: [RELEVANCE, ID]) {
      edges {
        role
        node {
          id
          image {
            large
            medium
          }
          name {
            userPreferred
          }
        }
      }
    }
    recommendations(sort: RATING_DESC) {
      nodes {
        mediaRecommendation {
          ${standardMediaInformation()}
        }
      }
    }
    externalLinks {
      url
      site
    }
  }
  Page(page: 1) {
    $standardPageInformation
    mediaList(isFollowing: true, sort: [STATUS], mediaId: $id) {
      id
      status
      score(format: POINT_100)
      progress
      progressVolumes
      user {
        id
        name
        avatar {
          large
          medium
        }
      }
    }
  }
}

""".prepare()