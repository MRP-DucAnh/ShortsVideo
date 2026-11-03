package com.shorts.flowdrama.domain

fun episodeAccessKey(showId: String, number: Int) = "$showId:$number"

fun Episode.withLocalAccess(showId: String, unlockedIds: Set<String>): Episode =
    if (!isFree && episodeAccessKey(showId, number) in unlockedIds) copy(isFree = true) else this
