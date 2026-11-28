import type { ReactNode } from 'react'
import styles from './icons.module.css'

export type IconSize = 'default' | 'small' | 'rail'

type IconProps = { size?: IconSize }

function SvgIcon({
  size = 'default',
  children,
}: IconProps & { children: ReactNode }) {
  return (
    <svg
      className={styles[size]}
      viewBox="0 0 20 20"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {children}
    </svg>
  )
}

export function PlusIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M10 4v12M4 10h12" />
    </SvgIcon>
  )
}

export function CloseIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M5 5l10 10M15 5L5 15" />
    </SvgIcon>
  )
}

export function ChevDownIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M5 8l5 5 5-5" />
    </SvgIcon>
  )
}

export function ChevRightIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M8 5l5 5-5 5" />
    </SvgIcon>
  )
}

export function ChevLeftIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M12 5l-5 5 5 5" />
    </SvgIcon>
  )
}

export function ArrowUpIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M10 16V4M5 9l5-5 5 5" />
    </SvgIcon>
  )
}

export function ArrowDownIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M10 4v12M5 11l5 5 5-5" />
    </SvgIcon>
  )
}

export function TrashIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M4 6h12M8 6V4h4v2M6 6l1 10h6l1-10M9 9v4.5M11 9v4.5" />
    </SvgIcon>
  )
}

export function PencilIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M4 16l1-4 8-8 3 3-8 8zM11 6l3 3" />
    </SvgIcon>
  )
}

export function SearchIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="9" cy="9" r="5.5" />
      <path d="M13 13l4 4" />
    </SvgIcon>
  )
}

export function LinkIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M8.5 11.5l3-3M9 6.5l1.5-1.5a3 3 0 0 1 4.2 4.2l-1.5 1.5M11 13.5l-1.5 1.5a3 3 0 0 1-4.2-4.2l1.5-1.5" />
    </SvgIcon>
  )
}

export function UserOffIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="9" cy="7" r="3" />
      <path d="M3.5 16c.6-2.8 2.8-4.5 5.5-4.5M13 13l4 4M17 13l-4 4" />
    </SvgIcon>
  )
}

export function UserCheckIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="9" cy="7" r="3" />
      <path d="M3.5 16c.6-2.8 2.8-4.5 5.5-4.5M12.5 15l2 2 3.5-4" />
    </SvgIcon>
  )
}

export function BanIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="10" cy="10" r="7" />
      <path d="M5 15L15 5" />
    </SvgIcon>
  )
}

export function HomeIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M3.5 9.5L10 4l6.5 5.5V16h-4.5v-4h-4v4H3.5z" />
    </SvgIcon>
  )
}

export function BookIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M10 6v10M10 6C8.5 4.8 6.5 4.5 4 4.5v10c2.5 0 4.5.3 6 1.5 1.5-1.2 3.5-1.5 6-1.5v-10c-2.5 0-4.5.3-6 1.5z" />
    </SvgIcon>
  )
}

export function PeopleIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="7.5" cy="7" r="2.5" />
      <path d="M3 15.5c.4-2.3 2.2-3.8 4.5-3.8s4.1 1.5 4.5 3.8" />
      <circle cx="13.5" cy="7.5" r="2" />
      <path d="M13.5 11.7c1.8 0 3.2 1.2 3.5 3.3" />
    </SvgIcon>
  )
}

export function ProfileIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="10" cy="10" r="7" />
      <circle cx="10" cy="8.5" r="2.5" />
      <path d="M5.5 15.2c.9-1.6 2.6-2.6 4.5-2.6s3.6 1 4.5 2.6" />
    </SvgIcon>
  )
}

export function LogOutIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M8 4H5v12h3M12 6.5l3.5 3.5-3.5 3.5M15.5 10H8" />
    </SvgIcon>
  )
}

export function GlobeIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="10" cy="10" r="7" />
      <path d="M3 10h14M10 3c2 2 2.8 4.4 2.8 7s-.8 5-2.8 7c-2-2-2.8-4.4-2.8-7S8 5 10 3z" />
    </SvgIcon>
  )
}

export function MenuIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M3.5 6h13M3.5 10h13M3.5 14h13" />
    </SvgIcon>
  )
}

export function SidebarIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <rect x="3.5" y="4" width="13" height="12" rx="1.5" />
      <path d="M8 4v12" />
    </SvgIcon>
  )
}

export function TTheoryIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M5 3.5h7l3 3v10H5zM12 3.5v3h3M7.5 10h5M7.5 13h5" />
    </SvgIcon>
  )
}

export function TImageIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <rect x="3.5" y="4.5" width="13" height="11" rx="1.5" />
      <circle cx="7.5" cy="8.5" r="1.25" />
      <path d="M4 14l4-4 3 3 2-2 3.5 3.5" />
    </SvgIcon>
  )
}

export function TPickIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <rect x="3.5" y="6" width="13" height="8" rx="1.5" />
      <path d="M11 9.25l1.5 1.5 1.5-1.5M6 10h3" />
    </SvgIcon>
  )
}

export function TTypeIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M10 5v10M7 3.5c1.6 0 3 .6 3 1.5 0-.9 1.4-1.5 3-1.5M7 16.5c1.6 0 3-.6 3-1.5 0 .9 1.4 1.5 3 1.5" />
    </SvgIcon>
  )
}

export function TAudioIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M4 8h2.5L10 5v10l-3.5-3H4zM13 7.5c.8.7 1.2 1.6 1.2 2.5s-.4 1.8-1.2 2.5M15 5.5c1.3 1.2 2 2.8 2 4.5s-.7 3.3-2 4.5" />
    </SvgIcon>
  )
}

export function PlayIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M7 5v10l8-5z" fill="currentColor" stroke="none" />
    </SvgIcon>
  )
}

export function PauseIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M7 5v10M13 5v10" />
    </SvgIcon>
  )
}

export function MoreIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="5" cy="10" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="10" cy="10" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="15" cy="10" r="1.4" fill="currentColor" stroke="none" />
    </SvgIcon>
  )
}

export function CheckIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M4.5 10.5l3.5 3.5 7.5-8" />
    </SvgIcon>
  )
}

export function RightIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="10" cy="10" r="7" />
      <path d="M7 10.2l2 2 4-4.4" />
    </SvgIcon>
  )
}

export function WrongIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="10" cy="10" r="7" />
      <path d="M7.5 7.5l5 5M12.5 7.5l-5 5" />
    </SvgIcon>
  )
}

export function ClockIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="10" cy="10" r="7" />
      <path d="M10 6v4l2.5 2" />
    </SvgIcon>
  )
}

export function LockIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <rect x="5" y="9" width="10" height="7.5" rx="1.5" />
      <path d="M7 9V7a3 3 0 0 1 6 0v2" />
    </SvgIcon>
  )
}

export function TodoIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="10" cy="10" r="7" strokeDasharray="2.5 2.5" />
    </SvgIcon>
  )
}

export function SendIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M3.5 10L16.5 4l-4 12.5-2.5-5.5zM10 11l6.5-7" />
    </SvgIcon>
  )
}

export function EyeIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M2.5 10s2.8-5 7.5-5 7.5 5 7.5 5-2.8 5-7.5 5-7.5-5-7.5-5z" />
      <circle cx="10" cy="10" r="2.25" />
    </SvgIcon>
  )
}

export function InfoIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <circle cx="10" cy="10" r="7" />
      <path d="M10 9v4.5M10 6.5v.01" />
    </SvgIcon>
  )
}

export function AlertIcon(props: IconProps) {
  return (
    <SvgIcon {...props}>
      <path d="M10 3.5l7 12.5H3zM10 8.5v3.5M10 14v.01" />
    </SvgIcon>
  )
}

export const icons = {
  plus: PlusIcon,
  close: CloseIcon,
  chevDown: ChevDownIcon,
  chevRight: ChevRightIcon,
  chevLeft: ChevLeftIcon,
  arrowUp: ArrowUpIcon,
  arrowDown: ArrowDownIcon,
  trash: TrashIcon,
  pencil: PencilIcon,
  search: SearchIcon,
  link: LinkIcon,
  userOff: UserOffIcon,
  userCheck: UserCheckIcon,
  ban: BanIcon,
  home: HomeIcon,
  book: BookIcon,
  people: PeopleIcon,
  profile: ProfileIcon,
  logOut: LogOutIcon,
  globe: GlobeIcon,
  menu: MenuIcon,
  sidebar: SidebarIcon,
  tTheory: TTheoryIcon,
  tImage: TImageIcon,
  tPick: TPickIcon,
  tType: TTypeIcon,
  tAudio: TAudioIcon,
  play: PlayIcon,
  pause: PauseIcon,
  more: MoreIcon,
  check: CheckIcon,
  right: RightIcon,
  wrong: WrongIcon,
  clock: ClockIcon,
  lock: LockIcon,
  todo: TodoIcon,
  send: SendIcon,
  eye: EyeIcon,
  info: InfoIcon,
  alert: AlertIcon,
} as const

export type IconName = keyof typeof icons
