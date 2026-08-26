export type Gender = 'MALE' | 'FEMALE' | 'OTHER'
export type Role = 'USER' | 'ADMIN'
export type FollowStatus = 'PENDING' | 'ACCEPTED'
export type NotificationType = 'LIKE' | 'COMMENT' | 'FOLLOW' | 'MESSAGE'
export type ResourceType = 'POST' | 'COMMENT' | 'USER' | 'CONVERSATION'

export type ApiErrorResponse = {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  traceId: string | null
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type RegisterRequest = {
  username: string
  email: string
  password: string
  displayName: string
  dateOfBirth: string
  bio?: string | null
  location?: string | null
  gender?: Gender | null
  isPrivate?: boolean | null
}

export type LoginRequest = {
  usernameOrEmail: string
  password: string
}

export type UserMeResponse = {
  id: string
  username: string
  email: string
  displayName: string
  bio: string | null
  profilePictureUrl: string | null
  dateOfBirth: string
  location: string | null
  gender: Gender | null
  isPrivate: boolean
  role: Role
  isActive: boolean
  isBanned: boolean
  createdAt: string
}

export type LoginResponse = {
  token: string
  user: UserMeResponse
}

export type UpdateProfileRequest = {
  displayName?: string | null
  bio?: string | null
  location?: string | null
  gender?: Gender | null
  isPrivate?: boolean | null
}

export type UserPublicResponse = {
  id: string
  username: string
  displayName: string
  bio: string | null
  profilePictureUrl: string | null
  location: string | null
  gender: Gender | null
  isPrivate: boolean
}

export type UserLimitedResponse = {
  id: string
  username: string
  displayName: string
  profilePictureUrl: string | null
  isPrivate: boolean
}

export type UserSummaryResponse = {
  id: string
  username: string
  displayName: string
  profilePictureUrl: string | null
  isPrivate: boolean
}

export type UserProfileResponse = UserPublicResponse | UserLimitedResponse

export type AdminUserResponse = {
  id: string
  username: string
  displayName: string
  email: string
  role: Role
  isActive: boolean
  isBanned: boolean
  isPrivate: boolean
  createdAt: string
}

export type CreatePostRequest = {
  content: string
}

export type PostResponse = {
  id: string
  authorId: string
  authorUsername: string | null
  authorDisplayName: string | null
  authorProfilePictureUrl: string | null
  content: string
  imageUrl: string | null
  likeCount: number
  commentCount: number
  createdAt: string
}

export type FeedPostDto = PostResponse

export type CreateCommentRequest = {
  content: string
}

export type CommentResponse = {
  id: string
  postId: string
  authorId: string
  authorUsername: string | null
  authorDisplayName: string | null
  authorProfilePictureUrl: string | null
  content: string
  createdAt: string
}

export type LikeResponse = {
  liked: boolean
  count: number
}

export type LikeCountResponse = {
  count: number
}

export type LikedResponse = {
  liked: boolean
}

export type FollowResponse = {
  followerId: string
  followeeId: string
  status: FollowStatus
  createdAt: string
}

export type FollowStateResponse = {
  following: boolean
  pending: boolean
}

export type FollowStatsResponse = {
  followers: number
  following: number
}

export type FollowUserResponse = {
  userId: string
  username: string | null
  displayName: string | null
  profilePictureUrl: string | null
  followedAt: string
}

export type FollowingIdsResponse = {
  ids: string[]
}

export type NotificationResponse = {
  id: string
  actorId: string
  type: NotificationType
  resourceType: ResourceType
  resourceId: string
  message: string
  read: boolean
  createdAt: string
}

export type UnreadCountResponse = {
  unreadCount: number
}

export type LastMessagePreview = {
  id: string
  senderId: string
  content: string
  createdAt: string
}

export type ConversationResponse = {
  conversationId: string
  otherUserId: string
  otherUsername: string | null
  otherDisplayName: string | null
  otherProfilePictureUrl: string | null
  lastMessage: LastMessagePreview | null
  lastMessageAt: string | null
  unreadCount: number
}

export type CreateMessageRequest = {
  content: string
}

export type MessageResponse = {
  id: string
  conversationId: string
  senderId: string
  content: string
  createdAt: string
}

export type ChatSendRequest = {
  conversationId: string
  content: string
}

export function isPublicProfile(
  profile: UserProfileResponse,
): profile is UserPublicResponse {
  return 'bio' in profile
}

export function isLimitedProfile(profile: UserProfileResponse): boolean {
  return !isPublicProfile(profile)
}
